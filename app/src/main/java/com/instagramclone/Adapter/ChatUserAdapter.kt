package com.instagramclone.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.instagramclone.MessageActivity
import com.instagramclone.Model.Chat
import com.instagramclone.Model.User
import com.instagramclone.R
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class ChatUserAdapter (private var mContext: Context, private var mUsers: List<User>) : RecyclerView.Adapter<ChatUserAdapter.ViewHolder> () {

    private var firebaseUser: FirebaseUser? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatUserAdapter.ViewHolder {

        val view = LayoutInflater.from(mContext).inflate(R.layout.chat_user_item_layout, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mUsers.size
    }

    override fun onBindViewHolder(holder: ChatUserAdapter.ViewHolder, position: Int) {

        val user = mUsers[position]
        firebaseUser = FirebaseAuth.getInstance().currentUser

        Picasso.get().load(user.getImage()).placeholder(R.drawable.profile).into(holder.profileImage)
        holder.username.text = user.getUsername()

        if (user.getStatus().equals("online")) {
            holder.img_on.visibility = View.VISIBLE
        }
        else {
            holder.img_on.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(mContext, MessageActivity::class.java)
            intent.putExtra("userid", user.getUid())
            intent.putExtra("name", user.getUsername())
            mContext.startActivity(intent)
        }

        lastMessage(user.getUid(), holder.last_msg)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var profileImage: CircleImageView
        var username: TextView
        var img_on: CircleImageView
        var last_msg: TextView

        init {
            profileImage = itemView.findViewById(R.id.chat_profile_image)
            img_on = itemView.findViewById(R.id.img_on)
            username = itemView.findViewById(R.id.chat_username)
            last_msg = itemView.findViewById(R.id.last_msg)
        }

    }

    private fun lastMessage(userid: String, last_msg: TextView) {
        var theLastMessage = "default"
        val reference = FirebaseDatabase.getInstance().getReference("Chats")

        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val chat = snapshot.getValue(Chat::class.java)
                    if (firebaseUser!!.uid == chat!!.getReceiver() && userid == chat.getSender() ||
                        userid == chat.getReceiver() && firebaseUser!!.uid == chat.getSender()) {
                        theLastMessage = chat.getMessage()
                    }
                }
                if (theLastMessage == "default") {
                    last_msg.text = ""
                }
                else {
                    if (theLastMessage.startsWith("https://firebasestorage.googleapis.com/v0/b/")) {
                        last_msg.text = "Image"
                    }
                    else {
                        last_msg.setText(theLastMessage)
                    }
                }
                theLastMessage = "default"
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

}