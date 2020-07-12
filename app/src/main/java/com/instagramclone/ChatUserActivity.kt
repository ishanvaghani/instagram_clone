package com.instagramclone

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.instagramclone.Adapter.ChatUserAdapter
import com.instagramclone.ImportantClasses.UserStatus
import com.instagramclone.Model.ChatList
import com.instagramclone.Model.User

class ChatUserActivity : AppCompatActivity() {

    private var chatUserAdapter: ChatUserAdapter? = null
    private var chatListList: MutableList<ChatList>? = null
    private var chatUserList: MutableList<User>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        setContentView(R.layout.activity_chat_user)

        val toolbar: Toolbar = findViewById(R.id.chat_user_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setTitle("")
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val recyclerView: RecyclerView = findViewById(R.id.chat_recycler_view)
        val linearlayoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = linearlayoutManager

        chatUserList = ArrayList()
        chatListList = ArrayList()

        val reference = FirebaseDatabase.getInstance().getReference("Chatlist").child(FirebaseAuth.getInstance().currentUser!!.uid)
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                (chatListList as ArrayList<ChatList>).clear()
                for (snapshot in dataSnapshot.children) {
                    val chatlist = snapshot.getValue(ChatList::class.java)
                    (chatListList as ArrayList<ChatList>).add(chatlist!!)
                }
                chatList(recyclerView)
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun chatList(recyclerView: RecyclerView) {
        chatUserList = ArrayList()
        val reference = FirebaseDatabase.getInstance().getReference("Users")

        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                (chatUserList as ArrayList<User>).clear()
                for (snapshot in dataSnapshot.children) {
                    val user = snapshot.getValue(User::class.java)
                    for (chatlist in (chatListList as ArrayList<ChatList>)) {
                        if (chatlist.getId().equals(user!!.getUid())) {
                            (chatUserList as ArrayList<User>).add(user)
                        }
                    }
                }
                chatUserAdapter = ChatUserAdapter(this@ChatUserActivity, (chatUserList as ArrayList<User>))
                recyclerView.adapter = chatUserAdapter
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