package com.instagramclone

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.instagramclone.Adapter.MessageAdapter
import com.instagramclone.ImportantClasses.UserStatus
import com.instagramclone.Model.Chat
import com.instagramclone.Model.User
import com.instagramclone.Notification.*
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import hani.momanii.supernova_emoji_library.Actions.EmojIconActions
import hani.momanii.supernova_emoji_library.Actions.EmojIconActions.KeyboardListener
import hani.momanii.supernova_emoji_library.Helper.EmojiconEditText
import kotlinx.android.synthetic.main.activity_message.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MessageActivity : AppCompatActivity() {

    var profile_image: CircleImageView? = null
    var username: TextView? = null
    var status_user: TextView? = null

    var fuser: FirebaseUser? = null
    var reference: DatabaseReference? = null
    var storageReference: StorageReference? = null

    var mchat: List<Chat>? = null

    var userid: String? = null
    var msg: String? = null
    var user_name: String? = null

    var seenListner: ValueEventListener? = null

    var btn_send: ImageButton? = null
    var btn_attach: ImageButton? = null
    var btn_emoji: ImageView? = null
    var text_send: EmojiconEditText? = null
    var rootView: View? = null
    var emojIcon: EmojIconActions? = null

    var apiService: APIService? = null
    var notify = false

    var filepath: Uri? = null
    var imgUri: Uri? = null
    var progressDialog: ProgressDialog? = null

    private var messageAdapter: MessageAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        setContentView(R.layout.activity_message)

        progressDialog = ProgressDialog(this)
        rootView = findViewById(R.id.root_view)

        val toolbar: Toolbar = findViewById(R.id.message_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setTitle("")
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        apiService = Client.getClient("https://fcm.googleapis.com/")!!.create(APIService::class.java)

        val recyclerView: RecyclerView
        recyclerView = findViewById(R.id.message_recyclerview)
        recyclerView.setHasFixedSize(true)
        val linearLayoutManager = LinearLayoutManager(applicationContext)
        linearLayoutManager.stackFromEnd = true
        recyclerView.setLayoutManager(linearLayoutManager)

        profile_image = findViewById(R.id.message_profile_image)
        username = findViewById(R.id.message_username)
        status_user = findViewById(R.id.message_status)
        btn_send = findViewById(R.id.btn_send)
        text_send = findViewById(R.id.emojicon_edit_text)
        btn_attach = findViewById(R.id.attach)

        userid = intent.getStringExtra("userid")
        user_name = intent.getStringExtra("name")

        username!!.setOnClickListener {
            val pref = getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
            pref.putString("profileId", userid)
            pref.apply()
            startActivity(Intent(this, UserProfileActivity::class.java))
        }

        profile_image!!.setOnClickListener {
            val pref = getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
            pref.putString("profileId", userid)
            pref.apply()
            startActivity(Intent(this, UserProfileActivity::class.java))
        }

        fuser = FirebaseAuth.getInstance().currentUser
        storageReference = FirebaseStorage.getInstance().reference

        btn_attach!!.setOnClickListener {
            notify = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        1
                    )
                } else {
                    chooseImage()
                }
            } else {
                chooseImage()
            }
        }

        btn_send!!.setOnClickListener {
            notify = true
            msg = text_send!!.getText().toString().trim()
            if (msg != "") {
                sendMessage(fuser!!.uid, userid!!, msg!!)
            } else {
                Toast.makeText(this, "You can't send empty message", Toast.LENGTH_SHORT).show()
            }
            text_send!!.setText("")
        }

        reference = FirebaseDatabase.getInstance().getReference("Users").child(userid!!)
        reference!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                val typingStatus: String = user!!.getTypingTo()
                if (typingStatus == fuser!!.uid) {
                    status_user!!.setText("typing...")
                } else {
                    status_user!!.setText(user.getStatus())
                }
                username!!.setText(user.getUsername())
                Picasso.get().load(user.getImage()).placeholder(R.drawable.profile)
                    .into(message_profile_image)
                readMessages(fuser!!.uid, userid!!, user.getImage(), recyclerView)
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })


        //emoji keyboard
        btn_emoji = findViewById(R.id.emogi)

        if (rootView != null) {
            emojIcon = EmojIconActions(
                this,
                rootView,
                text_send,
                btn_emoji,
                "#3F51B5",
                "#e8e8e8",
                "#f4f4f4"
            )
            emojIcon!!.ShowEmojIcon()
            emojIcon!!.setIconsIds(
                R.drawable.ic_keyboard_black_24dp,
                R.drawable.ic_sentiment_satisfied_black_24dp
            )
            emojIcon!!.setKeyboardListener(object : KeyboardListener {

                override fun onKeyboardOpen() {

                }

                override fun onKeyboardClose() {

                }
            })
        }

        text_send!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.toString().trim().length == 0) {
                    typingStatus("noOne")
                } else {
                    typingStatus(userid!!)
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })

        seenMessage(userid!!)
    }

    fun chooseImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.type = "image/*"
        startActivityForResult(intent, 101)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == Activity.RESULT_OK) {

            val uris: MutableList<Uri> = ArrayList()
            val clipData = data!!.clipData

            progressDialog!!.setMessage("Please wait...")
            progressDialog!!.setCanceledOnTouchOutside(false)

            if (clipData != null) {

                progressDialog!!.setTitle("Sending multiple Images")
                progressDialog!!.show()

                for (i in 0 until clipData.itemCount) {

                    filepath = clipData.getItemAt(i).uri

                    try {
                        uris.add(filepath!!)
                    } catch (e: Exception) {
                        progressDialog!!.dismiss()
                        e.printStackTrace()
                    }
                }
            } else {
                filepath = data.data
                progressDialog!!.setTitle("Sending Image")
                progressDialog!!.show()
                try {
                    uris.add(filepath!!)
                } catch (e: Exception) {
                    Toast.makeText(this, "" + e, Toast.LENGTH_SHORT).show()
                    progressDialog!!.dismiss()
                }
            }
            Thread(Runnable {
                for (b in uris) {
                    runOnUiThread {
                        sendFile(fuser!!.uid, userid!!, b)
                        uris.clear()
                    }
                }
            }).start()
        }
    }

    private fun seenMessage(userid: String) {
        reference = FirebaseDatabase.getInstance().getReference("Chats")

        seenListner = reference!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {

                    val chat = snapshot.getValue(Chat::class.java)

                    if (fuser!!.uid == chat!!.getReceiver() && userid == chat.getSender()) {

                        val hashMap = HashMap<String, Any>()
                        hashMap["isseen"] = true
                        snapshot.ref.updateChildren(hashMap)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun sendFile(sender: String, receiver: String, uri: Uri) {

        val id = UUID.randomUUID().toString()
        val msgab = FirebaseDatabase.getInstance().reference.push().key
        val sreference = storageReference!!.child("Message Pictures/$id")

        var reference = FirebaseDatabase.getInstance().reference
        val finalReference = reference
        sreference.putFile(uri).addOnSuccessListener {
            sreference.downloadUrl.addOnSuccessListener { uri ->

                imgUri = uri
                val imgurl = uri.toString()
                val svaetime: String
                val savedate: String
                val main: String

                val calendar = Calendar.getInstance()
                val currentDate = SimpleDateFormat("dd MMM")
                savedate = currentDate.format(calendar.time)
                val currentTime = SimpleDateFormat("hh:mm a")
                svaetime = currentTime.format(calendar.time)
                main = "$svaetime, $savedate"

                val chat = Chat(sender, receiver, imgurl, false, msgab!!, main)

                finalReference.child("Chats").child(msgab).setValue(chat)
                progressDialog!!.dismiss()
            }
        }

        val chatRef = FirebaseDatabase.getInstance().getReference("Chatlist")

        val hashMap1 = HashMap<String, String>()
        hashMap1["id"] = userid!!
        chatRef.child(fuser!!.uid).child(userid!!).setValue(hashMap1)

        val hashMap2 = HashMap<String, String>()
        hashMap2["id"] = sender
        chatRef.child(receiver).child(sender).setValue(hashMap2)

        //notification
        reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser!!.uid)
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                if (notify) {
                    sendNotification(receiver, user!!.getUsername(), "Image")
                }
                notify = false
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
        //--------------
    }

    private fun sendMessage(sender: String, receiver: String, message: String) {

        val msgab = FirebaseDatabase.getInstance().reference.push().key
        var reference = FirebaseDatabase.getInstance().reference

        val savetime: String
        val savedate: String
        val main: String

        val calendar = Calendar.getInstance()
        val currentDate = SimpleDateFormat("dd MMM")
        savedate = currentDate.format(calendar.time)
        val currentTime = SimpleDateFormat("hh:mm a")
        savetime = currentTime.format(calendar.time)
        main = "$savetime, $savedate"

        val hashMap = HashMap<String, Any?>()
        hashMap["sender"] = sender
        hashMap["receiver"] = receiver
        hashMap["message"] = message
        hashMap["isseen"] = false
        hashMap["msgid"] = msgab
        hashMap["time"] = main
        reference.child("Chats").child(msgab!!).setValue(hashMap)

        val chatRef = FirebaseDatabase.getInstance().getReference("Chatlist")
        val hashMap1 = HashMap<String, String>()
        hashMap1["id"] = userid!!
        chatRef.child(fuser!!.uid).child(userid!!).setValue(hashMap1)

        val hashMap2 = HashMap<String, String>()
        hashMap2["id"] = sender
        chatRef.child(receiver).child(sender).setValue(hashMap2)

        //notification
        reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser!!.uid)
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                if (notify) {
                    sendNotification(receiver, user!!.getUsername(), message)
                }
                notify = false
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
        //--------------
    }
    
    private fun sendNotification(receiver: String, username: String, message: String) {

        val tokens = FirebaseDatabase.getInstance().reference.child("Tokens")
        val query = tokens.orderByKey().equalTo(receiver)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val token = snapshot.getValue(Token::class.java)
                    val data = Data(fuser!!.uid, R.drawable.icon, "$username : $message", "New Message", userid!!)
                    val sender = Sender(data, token!!.getToken())
                    apiService!!.sendNotification(sender)
                        .enqueue(object : Callback<MyResponse> {
                            override fun onResponse(
                                call: Call<MyResponse>,
                                response: Response<MyResponse>
                            ) {
                                if (response.code() == 200) {
                                    if (response.body()!!.success !== 1) {

                                    }
                                }
                            }

                            override fun onFailure(
                                call: Call<MyResponse>,
                                t: Throwable
                            ) {
                            }
                        })
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun readMessages(
        myid: String,
        userid: String,
        imageurl: String,
        recyclerView: RecyclerView
    ) {

        mchat = ArrayList()
        reference = FirebaseDatabase.getInstance().getReference("Chats")

        reference!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                (mchat as ArrayList<Chat>).clear()

                for (snapshot in dataSnapshot.children) {

                    val chat = snapshot.getValue(Chat::class.java)

                    if (myid == chat!!.getReceiver() && userid == chat.getSender() ||
                        userid == chat.getReceiver() && myid == chat.getSender()
                    ) {

                        (mchat as ArrayList<Chat>).add(chat)

                    }

                    messageAdapter =
                        MessageAdapter(this@MessageActivity, (mchat as ArrayList<Chat>), imageurl)
                    recyclerView.adapter = messageAdapter!!
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun currentUser(userid: String) {
        val editor = getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
        editor.putString("currentuser", userid)
        editor.apply()
    }

    private fun typingStatus(typing: String) {
        reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser!!.uid)
        val hashMap = HashMap<String, Any>()
        hashMap["typingTo"] = typing
        reference!!.updateChildren(hashMap)
    }

    override fun onResume() {
        super.onResume()
        UserStatus.online()
        currentUser(userid!!)
    }

    override fun onPause() {
        super.onPause()
        reference!!.removeEventListener(seenListner!!)

        UserStatus.offline()

        typingStatus("noOne")
        currentUser("none")
    }
}
