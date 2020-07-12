package com.instagramclone.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.instagramclone.Model.Chat
import com.instagramclone.R
import com.squareup.picasso.Picasso

class MessageAdapter(private var mContext: Context, private var mChat: List<Chat>, private var imageurl: String) : RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

    private val MSG_TYPE_LEFT = 0
    private val MSG_TYPE_RIGHT = 1

    var fuser: FirebaseUser? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == MSG_TYPE_RIGHT) {
            val view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_right, parent, false)
            ViewHolder(view)
        } else {
            val view= LayoutInflater.from(mContext).inflate(R.layout.chat_item_left, parent, false)
            ViewHolder(view)
        }
    }


    override fun getItemCount(): Int {
        return mChat.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = mChat[position]
        if (chat.getMessage().startsWith("https://firebasestorage.googleapis.com/v0/b/")) {
            Picasso.get().load(chat.getMessage()).placeholder(R.drawable.profile).into(holder.message_image)
            holder.img_time!!.setText(chat.getTime())
            holder.msg_time!!.setText(chat.getTime())
            holder.message_image!!.setVisibility(View.VISIBLE)
            holder.msg_ly!!.setVisibility(View.GONE)
        } else {
            holder.msg_time!!.setText(chat.getTime())
            holder.show_message!!.setText(chat.getMessage())
            holder.img_ly!!.setVisibility(View.GONE)
        }

        Picasso.get().load(imageurl).placeholder(R.drawable.profile).into(holder.profile_image)

        if (position == mChat.size - 1) {
            if (chat.isIsseen()) {
                if (chat.getMessage().startsWith("https://firebasestorage.googleapis.com/v0/b/")) {
                    holder.image_seen!!.setText("seen")
                } else {
                    holder.txt_seen!!.setText("seen")
                }
            } else {
                if (chat.getMessage().startsWith("https://firebasestorage.googleapis.com/v0/b/")) {
                    holder.image_seen!!.setText("sent")
                } else {
                    holder.txt_seen!!.setText("sent")
                }
            }
        } else {
            holder.image_seen!!.setVisibility(View.GONE)
            holder.txt_seen!!.setVisibility(View.GONE)
        }

        //open image click
//        holder.message_image!!.setOnClickListener(View.OnClickListener {
//            val intent = Intent(holder.itemView.context, ImageViewActivity::class.java)
//            intent.putExtra("url", chat.getMessage())
//            holder.itemView.context.startActivity(intent)
//        })


        //option menu when msg long click"
        if (mChat[position].getSender().equals(fuser!!.uid)) {
            holder.msg_ly!!.setOnLongClickListener {
                val option = arrayOf<CharSequence>(
                    "Delete",
                    "Cancle"
                )
                val builder = AlertDialog.Builder(mContext)
                builder.setTitle("Delete Message?")

                builder.setItems(option) { dialog, which ->
                    if (which == 0) {

                        val reference = FirebaseDatabase.getInstance().getReference("Chats").child(chat.getMsgid()!!)
                        reference.removeValue()

                    }
                }
                builder.show()
                false
            }
            holder.message_image!!.setOnLongClickListener {
                val option = arrayOf<CharSequence>(
                    "Delete",
                    "Cancle"
                )
                val builder = AlertDialog.Builder(mContext)
                builder.setTitle("Delete Image?")

                builder.setItems(option) { dialog, which ->
                    if (which == 0) {
                        val reference = FirebaseDatabase.getInstance().getReference("Chats").child(chat.getMsgid()!!)
                        reference.removeValue()

                    }
                }
                builder.show()
                false
            }

        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var show_message: TextView? = null
        var msg_ly: LinearLayout? = null
        var img_ly:LinearLayout? = null
        var profile_image: ImageView? = null
        var txt_seen: TextView? = null
        var image_seen:TextView? = null
        var msg_time:TextView? = null
        var img_time:TextView? = null
        var message_image: ImageView? = null

        init {
            show_message = itemView.findViewById(R.id.show_message)
            profile_image = itemView.findViewById(R.id.profile_image)
            message_image = itemView.findViewById(R.id.show_image)
            txt_seen = itemView.findViewById(R.id.txt_seen)
            image_seen = itemView.findViewById(R.id.image_seen)
            msg_time = itemView.findViewById(R.id.msg_time)
            img_time = itemView.findViewById(R.id.img_time)
            msg_ly = itemView.findViewById(R.id.msg_layout)
            img_ly = itemView.findViewById(R.id.img_layout)
        }
    }

    override fun getItemViewType(position: Int): Int {
        fuser = FirebaseAuth.getInstance().currentUser
        return if (mChat[position].getSender().equals(fuser!!.getUid())) {
            MSG_TYPE_RIGHT
        } else {
            MSG_TYPE_LEFT
        }
    }
}