package com.instagramclone.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.instagramclone.Model.Post
import com.instagramclone.PostDetailsActivity
import com.instagramclone.R
import com.squareup.picasso.Picasso

class MyImageAdapter(private val mContext: Context, mPost: List<Post>) :
    RecyclerView.Adapter<MyImageAdapter.ViewHolder>() {

    private var mPost : List<Post>? = null
    init {
        this.mPost = mPost
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.images_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mPost!!.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post: Post = mPost!![position]
        Picasso.get().load(post.getImage()).into(holder.postImage)

        holder.postImage.setOnClickListener {
            val editor = mContext.getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
            editor.putString("postId", post.getPostId())
            editor.putString("publisherId", post.getPublisher())
            editor.apply()
            mContext.startActivity(Intent(mContext, PostDetailsActivity::class.java))

        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImage: ImageView

        init {
            postImage = itemView.findViewById(R.id.post_image)
        }
    }
}