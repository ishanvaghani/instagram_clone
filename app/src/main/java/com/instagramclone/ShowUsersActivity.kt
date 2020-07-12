package com.instagramclone

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.instagramclone.Adapter.UserAdapter
import com.instagramclone.ImportantClasses.UserStatus
import com.instagramclone.Model.User

class ShowUsersActivity : AppCompatActivity() {

    var id: String = ""
    var title: String = ""

    var userAdapter: UserAdapter? = null
    var userList: List<User>? = null
    var idList: List<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        setContentView(R.layout.activity_show_users)

        val intent = intent
        id = intent.getStringExtra("id")
        title = intent.getStringExtra("title")

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = title
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val recyclerView: RecyclerView
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        userList = ArrayList()
        userAdapter = UserAdapter(this, userList as ArrayList<User>, false)
        recyclerView.adapter = userAdapter

        idList = ArrayList()

        when(title) {
            "likes" -> getLikes()
            "following" -> getFollowing()
            "followers" -> getFollowers()
            "views" -> getView()
        }
    }

    private fun getView() {

        val ref = FirebaseDatabase.getInstance().reference.child("Story")
            .child(id).child(intent.getStringExtra("storyid")).child("views")

        ref.addValueEventListener(object : ValueEventListener {

            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {

                if (p0.exists()) {

                    (idList as ArrayList<String>).clear()

                    for(snapshot in p0.children) {
                        (idList as ArrayList<String>).add(snapshot.key!!)
                    }
                    showUsers()
                }
            }

        })
    }

    private fun getFollowers() {

        val followerRef = FirebaseDatabase.getInstance().reference.child("Follow")
            .child(id).child("Followers")

        followerRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {

                if (p0.exists()) {

                    (idList as ArrayList<String>).clear()

                    for(snapshot in p0.children) {
                        (idList as ArrayList<String>).add(snapshot.key!!)
                    }
                    showUsers()
                }
            }

        })
    }

    private fun getFollowing() {
        val followingRef = FirebaseDatabase.getInstance().reference.child("Follow")
            .child(id).child("Following")

        followingRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {

                    (idList as ArrayList<String>).clear()

                    for(snapshot in p0.children) {
                        (idList as ArrayList<String>).add(snapshot.key!!)
                    }
                    showUsers()
                }
            }
        })
    }

    private fun getLikes() {

        val likesRef = FirebaseDatabase.getInstance().reference.child("Likes").child(id)

        likesRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    (idList as ArrayList<String>).clear()

                    for(snapshot in p0.children) {
                        (idList as ArrayList<String>).add(snapshot.key!!)
                    }
                    showUsers()
                }
            }

        })
    }

    private fun showUsers() {
        val userRef = FirebaseDatabase.getInstance().reference.child("Users")

        userRef.addValueEventListener(object : ValueEventListener {

            override fun onCancelled(p0: DatabaseError) {
                TODO("Not yet implemented")
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {

                (userList as ArrayList<String>).clear()

                for(snapshot in dataSnapshot.children) {

                    val user = snapshot.getValue(User::class.java)

                    for(id in idList!!) {
                        if(user != null) {
                            if(user.getUid() == id) {
                                (userList as ArrayList<User>).add(user)
                            }
                        }
                    }
                }
                    userAdapter?.notifyDataSetChanged()

            }
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