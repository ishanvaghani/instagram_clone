package com.instagramclone

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.instagramclone.ImportantClasses.UserStatus
import com.instagramclone.Model.Post
import com.instagramclone.Model.User
import com.instagramclone.Notification.*
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PostDetailsActivity : AppCompatActivity(), PopupMenu.OnMenuItemClickListener {

    private var postId: String? = ""
    private var publisherId: String? = ""
    private var firebaseUser: FirebaseUser? = null
    var apiService: APIService? = null
    var post: Post? = null
    var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        setContentView(R.layout.activity_post_details)

        val toolbar: Toolbar = findViewById(R.id.post_details_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setTitle("")
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        firebaseUser = FirebaseAuth.getInstance().currentUser
        apiService = Client.getClient("https://fcm.googleapis.com/")!!.create(APIService::class.java)

        val prefrences = getSharedPreferences("PREFS", Context.MODE_PRIVATE)
        if(prefrences != null) {
            postId = prefrences.getString("postId", "none")
            publisherId = prefrences.getString("publisherId", "none")
        }

        val profile_image: CircleImageView = findViewById(R.id.post_details_user_image)
        val username: TextView = findViewById(R.id.post_details_username)
        val option_menu: ImageView = findViewById(R.id.post_details_more_options)
        val image: ImageView = findViewById(R.id.post_details_post_image)
        val like_btn: ImageView = findViewById(R.id.post_details_image_like_btn)
        val comment_btn: ImageView = findViewById(R.id.post_details_image_comment_btn)
        val save_btn: ImageView = findViewById(R.id.post_details_save_btn)
        val likes: TextView = findViewById(R.id.post_details_likes)
        val description: TextView = findViewById(R.id.post_details_description)
        val comment: TextView = findViewById(R.id.post_details_comments)

        publisherInfo(profile_image, username, publisherId!!)
        postInfo(image, description)
        isLike(like_btn)
        numberOfLikes(likes)
        numberOfComments(comment)
        checkSaveStatus(save_btn)

        if(publisherId.equals(firebaseUser!!.uid)) {
            option_menu.visibility = View.VISIBLE
        }
        else {
            option_menu.visibility = View.GONE
        }

        like_btn.setOnClickListener {
            if(like_btn.tag == "Like") {
                FirebaseDatabase.getInstance().reference.child("Likes").child(post!!.getPostId()).child(firebaseUser!!.uid).setValue(true)
                addNotification(post!!.getPublisher(), post!!.getPostId())
                sendNotification(post!!.getPublisher(), user!!.getUsername(), "Liked Your Post", post!!.getPostId())
            }
            else {
                FirebaseDatabase.getInstance().reference.child("Likes").child(post!!.getPostId()).child(firebaseUser!!.uid).removeValue()
                numberOfLikes(likes)
            }
        }

        var doubleClickLastTime = 0L
        image.setOnClickListener {
            if(System.currentTimeMillis() - doubleClickLastTime < 300){
                doubleClickLastTime = 0
                if(like_btn.tag == "Like") {
                    FirebaseDatabase.getInstance().reference.child("Likes").child(post!!.getPostId()).child(firebaseUser!!.uid).setValue(true)
                    addNotification(post!!.getPublisher(), post!!.getPostId())
                    sendNotification(post!!.getPublisher(), user!!.getUsername(), "Liked Your Post", post!!.getPostId())
                }
                else {
                    FirebaseDatabase.getInstance().reference.child("Likes").child(post!!.getPostId()).child(firebaseUser!!.uid).removeValue()
                    numberOfLikes(likes)
                }
            }else{
                doubleClickLastTime = System.currentTimeMillis()
            }
        }

        profile_image.setOnClickListener {
            val editor = getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
            editor.putString("profileId", post!!.getPublisher())
            editor.apply()
            startActivity(Intent(this, UserProfileActivity::class.java))
        }

        username.setOnClickListener {
            val editor = getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
            editor.putString("profileId", post!!.getPublisher())
            editor.apply()
            startActivity(Intent(this, UserProfileActivity::class.java))
        }

        comment_btn.setOnClickListener {
            val intent= Intent(this, CommentsActivity::class.java)
            intent.putExtra("postId", post!!.getPostId())
            intent.putExtra("PublisherId", post!!.getPublisher())
            startActivity(intent)
        }

        comment.setOnClickListener {
            val intent= Intent(this, CommentsActivity::class.java)
            intent.putExtra("postId", post!!.getPostId())
            intent.putExtra("PublisherId", post!!.getPublisher())
            startActivity(intent)
        }

        likes.setOnClickListener {
            val intent = Intent(this, ShowUsersActivity::class.java)
            intent.putExtra("id", post!!.getPostId())
            intent.putExtra("title", "likes")
            startActivity(intent)
        }

        save_btn.setOnClickListener {
            if(save_btn.tag == "Save") {
                FirebaseDatabase.getInstance().reference.child("Saves").child(firebaseUser!!.uid).child(post!!.getPostId()).setValue(true)
            }
            else {
                FirebaseDatabase.getInstance().reference.child("Saves").child(firebaseUser!!.uid).child(post!!.getPostId()).removeValue()
            }
        }

        option_menu.setOnClickListener {
            val popup = PopupMenu(this, it)
            popup.setOnMenuItemClickListener(this)
            popup.inflate(R.menu.post_options)
            popup.show()
        }

    }

    private fun isLike(likeBtn: ImageView) {
        val likesRef = FirebaseDatabase.getInstance().reference.child("Likes").child(postId!!)

        likesRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.child(firebaseUser!!.uid).exists()) {
                    likeBtn.setImageResource(R.drawable.heart_clicked)
                    likeBtn.tag = "Liked"
                }
                else {
                    likeBtn.setImageResource(R.drawable.heart_not_clicked)
                    likeBtn.tag = "Like"
                }
            }

        })
    }

    private fun publisherInfo(profileImage: CircleImageView, userName: TextView, publisherId: String) {
        val usersRef = FirebaseDatabase.getInstance().reference.child("Users").child(publisherId)

        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if(p0.exists()) {
                    user = p0.getValue(User::class.java)
                    Picasso.get().load(user!!.getImage()).placeholder(R.drawable.profile).into(profileImage)
                    userName.text = user!!.getUsername()
                }
            }
        })
    }

    private fun postInfo(postImage: ImageView, description: TextView) {
        val postRef = FirebaseDatabase.getInstance().reference.child("Posts").child(postId!!)

        postRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if(p0.exists()) {
                    post = p0.getValue(Post::class.java)
                    Picasso.get().load(post!!.getImage()).placeholder(R.drawable.profile).into(postImage)
                    description.text = post!!.getDescription()
                }
            }

        })
    }

    private fun sendNotification(receiver: String, username: String, message: String, postId: String) {

        val tokens = FirebaseDatabase.getInstance().reference.child("Tokens")
        val query = tokens.orderByKey().equalTo(receiver)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {

                    val token = snapshot.getValue(Token::class.java)
                    val data = Data(FirebaseAuth.getInstance().currentUser!!.uid, R.drawable.icon, "$username : $message", "New Like", receiver, postId)
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

    private fun numberOfLikes(likes: TextView) {
        val likesRef = FirebaseDatabase.getInstance().reference.child("Likes").child(postId!!)

        likesRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    likes.text = p0.childrenCount.toString() + " likes"
                }
                else {
                    likes.text = ""
                }
            }

        })
    }

    private fun numberOfComments(comments: TextView) {

        val commentsRef = FirebaseDatabase.getInstance().reference.child("Comments").child(postId!!)

        commentsRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    comments.text = "view all " + p0.childrenCount.toString() + " comments"
                }
                else {
                    comments.text = ""
                }
            }

        })
    }

    private fun checkSaveStatus(imageView: ImageView) {

        val saveRef = FirebaseDatabase.getInstance().reference.child("Saves").child(firebaseUser!!.uid)

        saveRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if(p0.child(postId!!).exists()) {
                    imageView.setImageResource(R.drawable.save_large_icon)
                    imageView.tag = "Saved"
                }
                else {
                    imageView.setImageResource(R.drawable.save_unfilled_large_icon)
                    imageView.tag = "Save"
                }
            }
        })

    }

    private fun addNotification(userId: String, postId: String) {

        val notiRef = FirebaseDatabase.getInstance().reference.child("Notifications").child(userId)
        val notiMap = HashMap<String, Any>()
        notiMap["userid"] = firebaseUser!!.uid
        notiMap["text"] = "liked your post"
        notiMap["postid"] = postId
        notiMap["ispost"] = true

        notiRef.push().setValue(notiMap)
    }

    private fun deletePost() {
        FirebaseDatabase.getInstance().reference.child("Posts").child(postId!!).removeValue()
        FirebaseDatabase.getInstance().reference.child("Likes").child(postId!!).removeValue()
        FirebaseDatabase.getInstance().reference.child("Comments").child(postId!!).removeValue()
        FirebaseDatabase.getInstance().reference.child("Saves").child(firebaseUser!!.uid).child(postId!!).removeValue()
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when {
            item!!.itemId.equals(R.id.edit_post) -> {
                val intent = Intent(this, AddPostActivity::class.java)
                intent.putExtra("postId", postId)
                startActivity(intent)
            }

            item.itemId.equals(R.id.delete_post) -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Delete Post?")
                builder.setPositiveButton("Yes") {
                    dialog, which -> deletePost()
                    finish()
                }
                builder.setNegativeButton("Cancel") {
                    dialog, which -> dialog.cancel()
                }
                builder.show()
            }
        }
        return false
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