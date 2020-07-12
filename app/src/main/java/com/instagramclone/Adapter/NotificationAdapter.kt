package com.instagramclone.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.instagramclone.Model.Notification
import com.instagramclone.Model.Post
import com.instagramclone.Model.User
import com.instagramclone.PostDetailsActivity
import com.instagramclone.R
import com.instagramclone.UserProfileActivity
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class NotificationAdapter(
    private val mContext: Context,
    private val mNotification: List<Notification>
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.notifications_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mNotification.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = mNotification[position]

        userInfo(holder.postImage, holder.username, notification.getUserId())

        if(notification.getText().equals("stared to following you")) {
            holder.text.text = "started following you"
        }
        else if(notification.getText().equals("liked your post")) {
            holder.text.text = "liked your post"
        }
        else if(notification.getText().contains("commented:")) {
            holder.text.text = notification.getText().replace("commented:", "commented: ")
        }
        else {
            holder.text.text = notification.getText()
        }

        if(notification.isIsPost()) {
            holder.postImage.visibility = View.VISIBLE
            getPostImage(holder.postImage, notification.getPostId())
        }
        else {
            holder.postImage.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            if(notification.isIsPost()) {
                val editor = mContext.getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
                editor.putString("postId", notification.getPostId())
                editor.apply()
                mContext.startActivity(Intent(mContext, PostDetailsActivity::class.java))
            }
            else {
                val editor = mContext.getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
                editor.putString("profileId", notification.getUserId())
                editor.apply()
                mContext.startActivity(Intent(mContext, UserProfileActivity::class.java))
            }
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var postImage: ImageView
        var profileImage: CircleImageView
        var username: TextView
        var text: TextView

        init {
            postImage = itemView.findViewById(R.id.notification_post_image)
            profileImage = itemView.findViewById(R.id.notification_profile_image)
            username = itemView.findViewById(R.id.username_notification)
            text = itemView.findViewById(R.id.comment_notification)
        }
    }

    private fun userInfo(imageView: ImageView, userName: TextView, publisherId: String) {
        val userRef = FirebaseDatabase.getInstance().reference.child("Users").child(publisherId)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    val user = p0.getValue<User>(User::class.java)
                    Picasso.get().load(user!!.getImage()).placeholder(R.drawable.profile)
                        .into(imageView)
                    userName.text = user.getUsername()

                }
            }
        })
    }

    private fun getPostImage(imageView: ImageView, postID: String) {
        val postRef = FirebaseDatabase.getInstance().reference.child("Posts").child(postID)

        postRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if(p0.exists()) {
                    val post = p0.getValue<Post>(Post::class.java)
                    Picasso.get().load(post!!.getImage()).placeholder(R.drawable.profile).into(imageView)
                }
            }
        })
    }
}