package com.instagramclone

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.instagramclone.Adapter.MyImageAdapter
import com.instagramclone.Model.Post
import com.instagramclone.Model.User
import com.instagramclone.ImportantClasses.UserStatus
import com.instagramclone.Notification.*
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_user_profile.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.collections.ArrayList

class UserProfileActivity : AppCompatActivity() {

    private lateinit var profileId: String
    private lateinit var firebaseUser: FirebaseUser

    var postList: List<Post>? = null
    var myImageAdapter: MyImageAdapter? = null

    var apiService: APIService? = null
    var currentUser: User? = null
    var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        setContentView(R.layout.activity_user_profile)

        val toolbar: Toolbar = findViewById(R.id.profile_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setTitle("")
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        apiService = Client.getClient("https://fcm.googleapis.com/")!!.create(APIService::class.java)

        val pref = getSharedPreferences("PREFS", Context.MODE_PRIVATE)
        firebaseUser = FirebaseAuth.getInstance().currentUser!!

        if (pref != null) {
            this.profileId = pref.getString("profileId", "none").toString()
        }

        // recycler view for uploaded image
        val recyclerViewUploadedImages: RecyclerView
        recyclerViewUploadedImages = findViewById(R.id.user_recycler_view_uploded_pic)
        recyclerViewUploadedImages.setHasFixedSize(true)
        val linearLayoutManager: LinearLayoutManager = GridLayoutManager(this, 3)
        recyclerViewUploadedImages.layoutManager = linearLayoutManager

        checkFollowAndFollowing()
        getFollowers()
        getFollowings()
        userInfo()
        myImages()
        getTotalNumberOfPost()
        currentUserInfo()

        postList = ArrayList()
        myImageAdapter =  MyImageAdapter(this, postList as ArrayList<Post>)
        recyclerViewUploadedImages.adapter = myImageAdapter

        user_layout_followers.setOnClickListener {
            val intent = Intent(this, ShowUsersActivity::class.java)
            intent.putExtra("id", profileId)
            intent.putExtra("title", "followers")
            startActivity(intent)
        }

        user_message_btn.setOnClickListener {
            
            val intent = Intent(this, MessageActivity::class.java)
            intent.putExtra("userid", profileId)
            startActivity(intent)
        }

        user_layout_following.setOnClickListener {
            val intent = Intent(this, ShowUsersActivity::class.java)
            intent.putExtra("id", profileId)
            intent.putExtra("title", "following")
            startActivity(intent)
        }

        user_edit_account_setting.setOnClickListener {
            val getButtonText = user_edit_account_setting.text.toString()
            when {

                getButtonText == "Follow" -> {
                    firebaseUser.uid.let { it1 ->
                        FirebaseDatabase.getInstance().reference.child("Follow")
                            .child(it1.toString()).child("Following").child(profileId)
                            .setValue(true)
                    }

                    firebaseUser.uid.let { it1 ->
                        FirebaseDatabase.getInstance().reference.child("Follow")
                            .child(profileId).child("Followers").child(it1.toString())
                            .setValue(true)
                    }

                    addNotification()
                    sendNotification(user!!.getUid(), currentUser!!.getUsername(), "Started following you")
                }
                getButtonText == "Following" -> {
                    firebaseUser.uid.let { it1 ->
                        FirebaseDatabase.getInstance().reference.child("Follow")
                            .child(it1.toString()).child("Following").child(profileId)
                            .removeValue()
                    }

                    firebaseUser.uid.let { it1 ->
                        FirebaseDatabase.getInstance().reference.child("Follow")
                            .child(profileId).child("Followers").child(it1.toString())
                            .removeValue()
                    }
                }
            }
        }
    }

    private fun myImages() {
        val postRef = FirebaseDatabase.getInstance().reference.child("Posts")

        postRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    (postList as ArrayList<Post>).clear()

                    for (snapshot in p0.children) {
                        val post = snapshot.getValue(Post::class.java)
                        if (post!!.getPublisher().equals(profileId)) {
                            (postList as ArrayList<Post>).add(post)
                        }
                        Collections.reverse(postList)
                        myImageAdapter!!.notifyDataSetChanged()
                    }
                }
            }

        })
    }

    private fun checkFollowAndFollowing() {
        val followingRef = firebaseUser.uid.let { it1 ->
            FirebaseDatabase.getInstance().reference.child("Follow")
                .child(it1).child("Following")
        }
        if (followingRef != null) {
            followingRef.addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {

                }

                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.child(profileId).exists()) {
                        user_edit_account_setting.text = "Following"
                    } else {
                        user_edit_account_setting.text = "Follow"
                    }
                }

            })
        }
    }

    private fun getFollowers() {
        val followerRef = FirebaseDatabase.getInstance().reference.child("Follow")
            .child(profileId).child("Followers")

        followerRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    user_total_follower.text = p0.childrenCount.toString()
                }
            }

        })
    }

    private fun getFollowings() {
        val followingRef = FirebaseDatabase.getInstance().reference.child("Follow")
            .child(profileId).child("Following")

        followingRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    user_total_following?.text = p0.childrenCount.toString()
                }
            }

        })
    }

    private fun userInfo() {
        val userRef = FirebaseDatabase.getInstance().reference.child("Users").child(profileId)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    user = p0.getValue(User::class.java)
                    Picasso.get().load(user!!.getImage()).placeholder(R.drawable.profile).into(user_profile_image_profile_frag)
                    user_profile_fragment_username.text = user!!.getUsername()
                    user_bio_profile_frag.text = user!!.getBio()
                    user_full_name_profile_frag.text = user!!.getFullname()
                }
            }
        })
    }

    private fun currentUserInfo() {
        val userRef = FirebaseDatabase.getInstance().reference.child("Users").child(FirebaseAuth.getInstance().currentUser!!.uid)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    currentUser = p0.getValue(User::class.java)
                }
            }
        })
    }

    private fun getTotalNumberOfPost() {
        val postsref = FirebaseDatabase.getInstance().reference.child("Posts")

        postsref.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    var postCount = 0

                    for (snapshot in p0.children) {
                        val post = snapshot.getValue(Post::class.java)

                        if (post!!.getPublisher() == profileId) {
                            postCount++
                        }
                    }

                    user_total_post.text = postCount.toString()
                }
            }

        })
    }

    private fun addNotification() {

        val notiRef = FirebaseDatabase.getInstance().reference.child("Notifications").child(profileId)
        val notiMap = HashMap<String, Any>()
        notiMap["userid"] = firebaseUser.uid
        notiMap["text"] = "started following you"
        notiMap["postid"] = ""
        notiMap["ispost"] = false

        notiRef.push().setValue(notiMap)
    }

    private fun sendNotification(receiver: String, username: String, message: String) {

        val tokens = FirebaseDatabase.getInstance().reference.child("Tokens")
        val query = tokens.orderByKey().equalTo(receiver)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {

                    val token = snapshot.getValue(Token::class.java)
                    val data = Data(FirebaseAuth.getInstance().currentUser!!.uid, R.drawable.icon, "$username : $message", "New Follower", receiver)
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

    override fun onResume() {
        super.onResume()
        UserStatus.online()
    }

    override fun onPause() {
        super.onPause()
        UserStatus.offline()
    }
}