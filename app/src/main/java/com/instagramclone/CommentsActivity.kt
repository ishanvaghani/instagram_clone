package com.instagramclone

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.instagramclone.Adapter.CommentAdapter
import com.instagramclone.ImportantClasses.UserStatus
import com.instagramclone.Model.Comment
import com.instagramclone.Model.User
import com.instagramclone.Notification.*
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_comments.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CommentsActivity : AppCompatActivity() {

    private var postId: String? = ""
    private var publisherId: String? = ""
    private var firebaseUser: FirebaseUser? = null
    private var commentAdapter: CommentAdapter? = null
    private var commentList: MutableList<Comment>? = null
    var apiService: APIService? = null
    var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        setContentView(R.layout.activity_comments)

        apiService = Client.getClient("https://fcm.googleapis.com/")!!.create(APIService::class.java)

        val toolbar: Toolbar = findViewById(R.id.comments_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setTitle("")
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        firebaseUser = FirebaseAuth.getInstance().currentUser!!

        val intent = intent
        postId = intent.getStringExtra("postId")
        publisherId = intent.getStringExtra("PublisherId")

        post_comment.setOnClickListener {
            if(!add_comment.text.toString().equals("")) {
                addComment()
                sendNotification(publisherId!!, user!!.getUsername(), "Commented on your post", postId!!)
            }
            else {
                Toast.makeText(this, "Comment is empty", Toast.LENGTH_SHORT).show()
            }
        }

        val recyclerView: RecyclerView = findViewById(R.id.recycler_view_comments)

        val linearLayoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = linearLayoutManager
        linearLayoutManager.stackFromEnd = true

        commentList = ArrayList()
        commentAdapter = CommentAdapter(this, commentList as ArrayList<Comment>)
        recyclerView.adapter = commentAdapter

        userInfo()
        readComment()
        getPostDescription()
        getUploadedUserInfo()
    }

    private fun addComment() {
        val commentsRef = FirebaseDatabase.getInstance().reference.child("Comments").child(postId!!)

        val commentsMap = HashMap<String, Any>()
        commentsMap["comment"] = add_comment.text.toString().trim()
        commentsMap["publisher"] = firebaseUser!!.uid

        commentsRef.push().setValue(commentsMap)

        addNotification()

        add_comment.text.clear()
    }

    private fun sendNotification(receiver: String, username: String, message: String, postId: String) {

        val tokens = FirebaseDatabase.getInstance().reference.child("Tokens")
        val query = tokens.orderByKey().equalTo(receiver)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {

                    val token = snapshot.getValue(Token::class.java)
                    val data = Data(FirebaseAuth.getInstance().currentUser!!.uid, R.drawable.icon, "$username : $message", "New Comment", receiver, postId)
                    val sender = Sender(data, token!!.getToken())

                    apiService!!.sendNotification(sender).enqueue(object : Callback<MyResponse> {
                        override fun onResponse(call: Call<MyResponse>, response: Response<MyResponse>) {
                            if (response.code() == 200) {
                                if (response.body()!!.success !== 1) {

                                }
                            }
                        }

                        override fun onFailure(call: Call<MyResponse>, t: Throwable) {
                        }
                    })
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun getPostDescription() {
        val postRef = FirebaseDatabase.getInstance().reference.child("Posts").child(postId!!).child("description")

        postRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if(p0.exists()) {
                    val desc = p0.value.toString()
                    description_comment.text = desc
                }
            }
        })
    }

    private fun getUploadedUserInfo() {
        val postRef = FirebaseDatabase.getInstance().reference.child("Users").child(publisherId!!)

        postRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if(p0.exists()) {
                    val user = p0.getValue<User>(User::class.java)
                    Picasso.get().load(user!!.getImage()).placeholder(R.drawable.profile).into(user_profile_image_comment)
                    user_name_comment.text = user.getUsername()
                }
            }
        })
    }

    private fun userInfo() {
        val userRef = FirebaseDatabase.getInstance().reference.child("Users").child(firebaseUser!!.uid)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if(p0.exists()) {
                    user = p0.getValue(User::class.java)
                    Picasso.get().load(user!!.getImage()).placeholder(R.drawable.profile).into(profile_image_comments)
                }
            }
        })
    }

    private fun readComment() {
        val commentsRef = FirebaseDatabase.getInstance().reference.child("Comments").child(postId!!)

        commentsRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if(p0.exists()) {
                    commentList!!.clear()

                    for(snapshot in p0.children) {
                        val comment = snapshot.getValue(Comment::class.java)
                        commentList!!.add(comment!!)
                    }
                    commentAdapter!!.notifyDataSetChanged()
                }
            }
        })
    }

    private fun addNotification() {

        val notiRef = FirebaseDatabase.getInstance().reference.child("Notifications").child(publisherId!!)
        val notiMap = HashMap<String, Any>()
        notiMap["userid"] = firebaseUser!!.uid
        notiMap["text"] = "commented: " + add_comment.text.toString()
        notiMap["postid"] = postId!!
        notiMap["ispost"] = true

        notiRef.push().setValue(notiMap)
    }

    override fun onResume() {
        super.onResume()
        UserStatus.online()
    }

    override fun onPause() {
        super.onPause()
        UserStatus.offline()
    }
}