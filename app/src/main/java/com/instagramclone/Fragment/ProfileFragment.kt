package com.instagramclone.Fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.instagramclone.AccountSettingActivity
import com.instagramclone.Adapter.MyImageAdapter
import com.instagramclone.Model.Post
import com.instagramclone.Model.User
import com.instagramclone.R
import com.instagramclone.ShowUsersActivity
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.fragment_profile.view.*
import java.util.*
import kotlin.collections.ArrayList

class ProfileFragment : Fragment() {

    private lateinit var firebaseUser: FirebaseUser

    var postList: List<Post>? = null
    var myImageAdapter: MyImageAdapter? = null

    var mySavedAdapter: MyImageAdapter? = null
    var postListSaved: List<Post>? = null
    var mySavedImg: List<String>? = null

    private var profile_image: CircleImageView? = null
    private var profile_username: TextView? = null
    private var profile_bio: TextView? = null
    private var profile_fullname: TextView? = null
    private var profile_total_post: TextView? = null
    private var profile_total_followers: TextView? = null
    private var profile_total_following: TextView? = null
    private var edit_profile: Button? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        profile_image = view.findViewById(R.id.profile_image_profile_frag)
        profile_username = view.findViewById(R.id.profile_fragment_username)
        profile_bio = view.findViewById(R.id.bio_profile_frag)
        profile_fullname = view.findViewById(R.id.full_name_profile_frag)
        profile_total_post = view.findViewById(R.id.total_post)
        profile_total_followers = view.findViewById(R.id.total_follower)
        profile_total_following = view.findViewById(R.id.total_following)
        edit_profile = view.findViewById(R.id.edit_account_setting)

        firebaseUser = FirebaseAuth.getInstance().currentUser!!

        // recycler view for uploaded image
        val recyclerViewUploadedImages: RecyclerView
        recyclerViewUploadedImages = view.findViewById(R.id.recycler_view_uploded_pic)
        recyclerViewUploadedImages.setHasFixedSize(true)
        val linearLayoutManager: LinearLayoutManager = GridLayoutManager(context, 3)
        recyclerViewUploadedImages.layoutManager = linearLayoutManager

        postList = ArrayList()
        myImageAdapter = context?.let { MyImageAdapter(it, postList as ArrayList<Post>) }
        recyclerViewUploadedImages.adapter = myImageAdapter


        //recycler view for saved images
        val recyclerViewSavedImages: RecyclerView
        recyclerViewSavedImages = view.findViewById(R.id.recycler_view_saved_pic)
        recyclerViewSavedImages.setHasFixedSize(true)
        val linearLayoutManager2: LinearLayoutManager = GridLayoutManager(context, 3)
        recyclerViewSavedImages.layoutManager = linearLayoutManager2

        postListSaved = ArrayList()
        mySavedAdapter = context?.let { MyImageAdapter(it, postListSaved as ArrayList<Post>) }
        recyclerViewSavedImages.adapter = mySavedAdapter

        val uploadedPostBtn: ImageButton
        uploadedPostBtn = view.findViewById(R.id.images_grid_view_btn)
        uploadedPostBtn.setOnClickListener {
            recyclerViewSavedImages.visibility = View.GONE
            recyclerViewUploadedImages.visibility = View.VISIBLE
        }

        val savedPostBtn: ImageButton
        savedPostBtn = view.findViewById(R.id.images_save_btn)
        savedPostBtn.setOnClickListener {
            recyclerViewSavedImages.visibility = View.VISIBLE
            recyclerViewUploadedImages.visibility = View.GONE
        }

        view.layout_followers.setOnClickListener {
            val intent = Intent(context, ShowUsersActivity::class.java)
            intent.putExtra("id", firebaseUser.uid)
            intent.putExtra("title", "followers")
            startActivity(intent)
        }

        view.layout_following.setOnClickListener {
            val intent = Intent(context, ShowUsersActivity::class.java)
            intent.putExtra("id", firebaseUser.uid)
            intent.putExtra("title", "following")
            startActivity(intent)
        }

        edit_profile!!.setOnClickListener {
            startActivity(Intent(context, AccountSettingActivity::class.java))
        }

        getFollowers()
        getFollowings()
        userInfo()
        myImages()
        getTotalNumberOfPost()
        mySaves()

        return view
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
                        if (post!!.getPublisher().equals(firebaseUser.uid)) {
                            (postList as ArrayList<Post>).add(post)
                        }
                        Collections.reverse(postList)
                        myImageAdapter!!.notifyDataSetChanged()
                    }
                }
            }

        })
    }

    private fun getFollowers() {
        val followerRef = FirebaseDatabase.getInstance().reference.child("Follow")
            .child(firebaseUser.uid).child("Followers")

        followerRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    profile_total_followers!!.text = p0.childrenCount.toString()
                }
            }

        })
    }

    private fun getFollowings() {
        val followingRef = FirebaseDatabase.getInstance().reference.child("Follow")
            .child(firebaseUser.uid).child("Following")

        followingRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    profile_total_following!!.text = p0.childrenCount.toString()
                }
            }

        })
    }

    private fun userInfo() {
        val userRef = FirebaseDatabase.getInstance().reference.child("Users").child(firebaseUser.uid)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    val user = p0.getValue(User::class.java)
                    Picasso.get().load(user!!.getImage()).placeholder(R.drawable.profile).into(profile_image)
                    profile_username!!.text = user.getUsername()
                    profile_bio!!.text = user.getBio()
                    profile_fullname!!.text = user.getFullname()
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

                        if (post!!.getPublisher() == firebaseUser.uid) {
                            postCount++
                        }
                    }
                    profile_total_post!!.text = postCount.toString()
                }
            }

        })
    }

    private fun mySaves() {
        mySavedImg = ArrayList()

        val savedRef =
            FirebaseDatabase.getInstance().reference.child("Saves").child(firebaseUser.uid)

        savedRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    for (snapshot in p0.children) {
                        (mySavedImg as ArrayList<String>).add(snapshot.key!!)
                    }
                    readSavedImagesData()
                }
            }
        })
    }

    private fun readSavedImagesData() {
        val postRef = FirebaseDatabase.getInstance().reference.child("Posts")

        postRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    (postListSaved as ArrayList<Post>).clear()

                    for (snapshot in p0.children) {
                        val post = snapshot.getValue(Post::class.java)
                        for (key in mySavedImg!!) {
                            if (post!!.getPostId() == key) {
                                (postListSaved as ArrayList<Post>).add(post)
                            }
                        }
                    }

                    mySavedAdapter!!.notifyDataSetChanged()
                }
            }

        })
    }

    override fun onStop() {
        super.onStop()
        val pref = context?.getSharedPreferences("PREFS", Context.MODE_PRIVATE)?.edit()
        pref!!.putString("profileId", firebaseUser.uid)
        pref.apply()
    }

    override fun onPause() {
        super.onPause()
        val pref = context?.getSharedPreferences("PREFS", Context.MODE_PRIVATE)?.edit()
        pref!!.putString("profileId", firebaseUser.uid)
        pref.apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        val pref = context?.getSharedPreferences("PREFS", Context.MODE_PRIVATE)?.edit()
        pref!!.putString("profileId", firebaseUser.uid)
        pref.apply()
    }
}