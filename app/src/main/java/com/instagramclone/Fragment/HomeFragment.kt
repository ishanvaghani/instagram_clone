package com.instagramclone.Fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.instagramclone.Adapter.PostAdapter
import com.instagramclone.Adapter.StoryAdapter
import com.instagramclone.ChatUserActivity
import com.instagramclone.Model.Post
import com.instagramclone.Model.Story
import com.instagramclone.R

class HomeFragment : Fragment() {

    private var postAdapter: PostAdapter? = null
    private var postList: MutableList<Post>? = null
    private var followingList: MutableList<String>? = null

    private var storyAdapter: StoryAdapter? = null
    private var storyList: MutableList<Story>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view =  inflater.inflate(R.layout.fragment_home, container, false)

        // recyclerview posts
        val recyclerView: RecyclerView
        recyclerView = view.findViewById(R.id.recycler_view_home)
        val linearlayoutManager = LinearLayoutManager(context)
        linearlayoutManager.reverseLayout = true
        linearlayoutManager.stackFromEnd = true
        recyclerView.layoutManager = linearlayoutManager

        postList = ArrayList()
        postAdapter = context?.let { PostAdapter(it, postList as ArrayList<Post>) }
        recyclerView.adapter = postAdapter

        // recyclerview story
        val recyclerViewStory: RecyclerView
        recyclerViewStory = view.findViewById(R.id.recycler_view_story)
        recyclerViewStory.setHasFixedSize(true)
        val linearlayoutManager2 = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerViewStory.layoutManager = linearlayoutManager2

        storyList = ArrayList()
        storyAdapter = context?.let { StoryAdapter(it, storyList as ArrayList<Story>) }
        recyclerViewStory.adapter = storyAdapter

        val chat_button: ImageView
        chat_button = view.findViewById(R.id.chat_users_btn)
        chat_button.setOnClickListener {
            startActivity(Intent(context, ChatUserActivity::class.java))
        }

        checkFollowings()

        return view
    }

    private fun checkFollowings() {
        followingList = ArrayList()

        val followigRef = FirebaseDatabase.getInstance().reference
            .child("Follow").child(FirebaseAuth.getInstance().currentUser!!.uid)
            .child("Following")

        followigRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if(p0.exists()) {
                    (followingList as ArrayList<String>).clear()
                    for (snapshot in p0.children) {
                        snapshot.key?.let { (followingList as ArrayList<String>).add(it) }
                    }
                    retriveStories()
                    retrivePost()
                }
            }

        })
    }

    private fun retrivePost() {
        val postRef = FirebaseDatabase.getInstance().reference
            .child("Posts")

        postRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                postList?.clear()
                for(snapshot in p0.children) {
                    val post = snapshot.getValue(Post::class.java)

                    for(userId in (followingList as ArrayList<String>)) {
                        if(post!!.getPublisher().equals(userId)) {
                            postList!!.add(post)
                        }
                        postAdapter!!.notifyDataSetChanged()
                    }
                }
            }

        })
    }

    private fun retriveStories() {
        val storyRef = FirebaseDatabase.getInstance().reference
            .child("Story")

        storyRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                val timeCurrent = System.currentTimeMillis()

                (storyList as ArrayList<Story>).clear()

                (storyList as ArrayList<Story>).add(Story("", 0, 0, "", FirebaseAuth.getInstance().currentUser!!.uid))

                for(id in followingList!!) {

                    var countStory = 0
                    var story: Story? = null

                    for(snapshot in p0.child(id).children) {
                        story = snapshot.getValue(Story::class.java)
                        if(timeCurrent>story!!.getTimeStart() && timeCurrent<story.getTimeEnd()) {
                            countStory++
                        }
                    }
                    if(countStory>0) {
                        (storyList as ArrayList<Story>).add(story!!)
                    }
                }
                storyAdapter!!.notifyDataSetChanged()
            }
        })
    }
}