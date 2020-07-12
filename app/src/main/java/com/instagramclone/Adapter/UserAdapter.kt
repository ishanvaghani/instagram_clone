package com.instagramclone.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.instagramclone.Model.User
import com.instagramclone.Notification.*
import com.instagramclone.R
import com.instagramclone.UserProfileActivity
import com.squareup.picasso.Picasso
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserAdapter(private var mContext: Context, private var mUser: List<User>, private var isFragment: Boolean = false) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {

    private  var firebaseUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser
    var apiService: APIService? = null
    var currentUser: User? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.user_item_layout, parent, false)
        apiService = Client.getClient("https://fcm.googleapis.com/")!!.create(APIService::class.java)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mUser.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = mUser[position]

        holder.userName.text = user.getUsername()
        holder.userFullname.text = user.getFullname()
        Picasso.get().load(user.getImage()).placeholder(R.drawable.profile).into(holder.userProfileImage)

        checkFollowingStatus(user.getUid(), holder.followButton)
        currentUserInfo()

        holder.itemView.setOnClickListener {
            if(isFragment) {
                val pref = mContext.getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
                pref.putString("profileId", user.getUid())
                pref.apply()
                mContext.startActivity(Intent(mContext, UserProfileActivity::class.java))
            }
            else {
                val pref = mContext.getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
                pref.putString("profileId", user.getUid())
                pref.apply()
                mContext.startActivity(Intent(mContext, UserProfileActivity::class.java))
            }
        }

        holder.followButton.setOnClickListener {
            if (holder.followButton.text.toString() == "Follow") {
                firebaseUser?.uid.let { it1 ->
                    FirebaseDatabase.getInstance().reference.child("Follow")
                        .child(it1.toString()).child("Following")
                        .child(user.getUid()).setValue(true)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                firebaseUser?.uid.let { it1 ->
                                    FirebaseDatabase.getInstance().reference.child("Follow")
                                        .child(user.getUid()).child("Followers")
                                        .child(it1.toString()).setValue(true)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                sendNotification(user.getUid(), currentUser!!.getUsername(), "Started following you")
                                            }
                                        }
                                }
                            }
                        }
                }
                addNotification(user.getUid())
            }
            else {
                firebaseUser?.uid.let { it1 ->
                    FirebaseDatabase.getInstance().reference.child("Follow")
                        .child(it1.toString()).child("Following")
                        .child(user.getUid()).removeValue()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                firebaseUser?.uid.let { it1 ->
                                    FirebaseDatabase.getInstance().reference.child("Follow")
                                        .child(user.getUid()).child("Followers")
                                        .child(it1.toString()).removeValue()
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {

                                            }
                                        }
                                }
                            }
                        }
                }
            }
        }
    }

    private fun checkFollowingStatus(uid: String, followButton: Button) {
        val followingRef = firebaseUser?.uid.let { it ->
            FirebaseDatabase.getInstance().reference.child("Follow")
                .child(it.toString()).child("Following")
        }

        followingRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(dataSnapshot.child(uid).exists()) {
                    followButton.text = "Following"
                }
                else {
                    followButton.text = "Follow"
                }
            }
        })
    }

    private fun sendNotification(receiver: String, username: String, message: String) {

        val tokens = FirebaseDatabase.getInstance().reference.child("Tokens")
        val query = tokens.orderByKey().equalTo(receiver)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {

                    val token = snapshot.getValue(Token::class.java)
                    val data = Data(FirebaseAuth.getInstance().currentUser!!.uid, R.drawable.icon, "$username : $message", "New Follower", receiver)
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

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var userName: TextView = itemView.findViewById(R.id.user_name_search)
        var userFullname: TextView = itemView.findViewById(R.id.user_full_name_search)
        var userProfileImage: ImageView = itemView.findViewById(R.id.user_profile_image_search)
        var followButton: Button = itemView.findViewById(R.id.follow_btn_search)

    }

    private fun currentUserInfo() {
        val userRef = FirebaseDatabase.getInstance().reference.child("Users").child(firebaseUser!!.uid)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    currentUser = p0.getValue(User::class.java)
                }
            }
        })
    }

    private fun addNotification(userId: String) {

        val notiRef = FirebaseDatabase.getInstance().reference.child("Notifications").child(userId)
        val notiMap = HashMap<String, Any>()
        notiMap["userid"] = firebaseUser!!.uid
        notiMap["text"] = "started following you"
        notiMap["postid"] = ""
        notiMap["ispost"] = false

        notiRef.push().setValue(notiMap)
    }

}