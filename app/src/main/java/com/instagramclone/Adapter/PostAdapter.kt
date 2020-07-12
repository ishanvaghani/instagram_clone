package com.instagramclone.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.instagramclone.CommentsActivity
import com.instagramclone.Model.Post
import com.instagramclone.Model.User
import com.instagramclone.Notification.*
import com.instagramclone.R
import com.instagramclone.ShowUsersActivity
import com.instagramclone.UserProfileActivity
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PostAdapter(private val mContext: Context, private val mPost: List<Post>) : RecyclerView.Adapter<PostAdapter.ViewHolder>() {

    private var firebaseUser: FirebaseUser? = null
    var apiService: APIService? = null
    var user: User? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.post_layout, parent, false)
        apiService = Client.getClient("https://fcm.googleapis.com/")!!.create(APIService::class.java)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mPost.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        firebaseUser = FirebaseAuth.getInstance().currentUser

        val post = mPost[position]

        Picasso.get().load(post.getImage()).into(holder .postImage)
        if(!post.getDescription().equals("")) {
            holder.description.text = post.getDescription()
        }

        publisherInfo(holder.profileImage, holder.userName, post.getPublisher())
        isLike(post.getPostId(), holder.likeButton)
        numberOfLikes(holder.likes, post.getPostId())
        numberOfComments(holder.comments, post.getPostId())
        checkSaveStatus(post.getPostId(), holder.saveButton)
        userInfo()

        holder.likeButton.setOnClickListener {
            if(holder.likeButton.tag == "Like") {
                FirebaseDatabase.getInstance().reference.child("Likes").child(post.getPostId()).child(firebaseUser!!.uid).setValue(true)
                addNotification(post.getPublisher(), post.getPostId())
                sendNotification(post.getPublisher(), user!!.getUsername(), "Liked Your Post", post.getPostId())
            }
            else {
                FirebaseDatabase.getInstance().reference.child("Likes").child(post.getPostId()).child(firebaseUser!!.uid).removeValue()
                numberOfLikes(holder.likes, post.getPostId())
            }
        }

        var doubleClickLastTime = 0L
        holder.postImage.setOnClickListener {
            if(System.currentTimeMillis() - doubleClickLastTime < 300){
                doubleClickLastTime = 0
                if(holder.likeButton.tag == "Like") {
                    FirebaseDatabase.getInstance().reference.child("Likes").child(post.getPostId()).child(firebaseUser!!.uid).setValue(true)
                    addNotification(post.getPublisher(), post.getPostId())
                    sendNotification(post.getPublisher(), user!!.getUsername(), "Liked Your Post", post.getPostId())
                }
                else {
                    FirebaseDatabase.getInstance().reference.child("Likes").child(post.getPostId()).child(firebaseUser!!.uid).removeValue()
                    numberOfLikes(holder.likes, post.getPostId())
                }
            }else{
                doubleClickLastTime = System.currentTimeMillis()
            }
        }

        holder.profileImage.setOnClickListener {
            val editor = mContext.getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
            editor.putString("profileId", post.getPublisher())
            editor.apply()
            mContext.startActivity(Intent(mContext, UserProfileActivity::class.java))
        }

        holder.userName.setOnClickListener {
            val editor = mContext.getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
            editor.putString("profileId", post.getPublisher())
            editor.apply()
            mContext.startActivity(Intent(mContext, UserProfileActivity::class.java))
        }

        holder.commentButton.setOnClickListener {
            val intent= Intent(mContext, CommentsActivity::class.java)
            intent.putExtra("postId", post.getPostId())
            intent.putExtra("PublisherId", post.getPublisher())
            mContext.startActivity(intent)
        }

        holder.comments.setOnClickListener {
            val intent= Intent(mContext, CommentsActivity::class.java)
            intent.putExtra("postId", post.getPostId())
            intent.putExtra("PublisherId", post.getPublisher())
            mContext.startActivity(intent)
        }

        holder.likes.setOnClickListener {
            val intent = Intent(mContext, ShowUsersActivity::class.java)
            intent.putExtra("id", post.getPostId())
            intent.putExtra("title", "likes")
            mContext.startActivity(intent)
        }

        holder.saveButton.setOnClickListener {
            if(holder.saveButton.tag == "Save") {
                FirebaseDatabase.getInstance().reference.child("Saves").child(firebaseUser!!.uid).child(post.getPostId()).setValue(true)
            }
            else {
                FirebaseDatabase.getInstance().reference.child("Saves").child(firebaseUser!!.uid).child(post.getPostId()).removeValue()
            }
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var profileImage: CircleImageView
        var postImage: ImageView
        var commentButton: ImageView
        var likeButton: ImageView
        var saveButton: ImageView
        var userName: TextView
        var likes: TextView
        var description: TextView
        var comments: TextView

        init {
            profileImage = itemView.findViewById(R.id.user_profile_image_post)
            postImage = itemView.findViewById(R.id.post_image_home)
            commentButton = itemView.findViewById(R.id.post_image_comment_btn)
            likeButton = itemView.findViewById(R.id.post_image_like_btn)
            saveButton = itemView.findViewById(R.id.post_save_comment_btn)
            userName = itemView.findViewById(R.id.user_name_post)
            likes = itemView.findViewById(R.id.likes)
            description = itemView.findViewById(R.id.description)
            comments = itemView.findViewById(R.id.comments)
        }
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

    private fun numberOfLikes(likes: TextView, postId: String) {
        val likesRef = FirebaseDatabase.getInstance().reference.child("Likes").child(postId)

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

    private fun isLike(postId: String, likeButton: ImageView) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val likesRef = FirebaseDatabase.getInstance().reference.child("Likes").child(postId)

        likesRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.child(firebaseUser!!.uid).exists()) {
                    likeButton.setImageResource(R.drawable.heart_clicked)
                    likeButton.tag = "Liked"
                }
                else {
                    likeButton.setImageResource(R.drawable.heart_not_clicked)
                    likeButton.tag = "Like"
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
                    val user = p0.getValue(User::class.java)
                    Picasso.get().load(user!!.getImage()).placeholder(R.drawable.profile).into(profileImage)
                    userName.text = user.getUsername()
                }
            }
        })
    }

    private fun numberOfComments(comments: TextView, postId: String) {

        val commentsRef = FirebaseDatabase.getInstance().reference.child("Comments").child(postId)

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

    private fun checkSaveStatus(postId: String, imageView: ImageView) {

        val saveRef = FirebaseDatabase.getInstance().reference.child("Saves").child(firebaseUser!!.uid)

        saveRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if(p0.child(postId).exists()) {
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
}