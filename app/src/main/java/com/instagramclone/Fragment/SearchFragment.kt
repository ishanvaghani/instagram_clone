package com.instagramclone.Fragment

import android.os.Bundle
import android.os.TokenWatcher
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.instagramclone.Adapter.UserAdapter
import com.instagramclone.Model.User
import com.instagramclone.R
import kotlinx.android.synthetic.main.fragment_search.view.*

class SearchFragment : Fragment() {

    private var recyclerView: RecyclerView? = null
    private var userAdapter: UserAdapter? = null
    private var mUsers: MutableList<User>? = null
    private var firebaseUser: FirebaseUser? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.fragment_search, container, false)

        firebaseUser = FirebaseAuth.getInstance().currentUser

        recyclerView = view.findViewById(R.id.recycler_view_search)
        recyclerView?.setHasFixedSize(true)
        recyclerView?.layoutManager = LinearLayoutManager(context)

        mUsers = ArrayList()
        userAdapter = context?.let {
            UserAdapter(it, mUsers as ArrayList<User>, true)
        }
        recyclerView?.adapter = userAdapter

        view.search_edit_text.addTextChangedListener ( object: TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(view.search_edit_text.text.toString() == "") {
                    //
                }
                else {
                    recyclerView?.visibility = View.VISIBLE
                    retriveUser()
                    searchUser(s.toString().toLowerCase())
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }

        })

        return  view
    }

    private fun searchUser(input: String) {
        val query = FirebaseDatabase.getInstance().reference.child("Users")
            .orderByChild("search")
            .startAt(input)
            .endAt(input + "\uf8ff")

        query.addValueEventListener(object : ValueEventListener {

            override fun onCancelled(p0: DatabaseError) {
                TODO("Not yet implemented")
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {

                mUsers?.clear()

                for(snapshot in dataSnapshot.children) {
                    val user = snapshot.getValue(User::class.java)
                    if(user != null) {

                        if(user.getUid() != firebaseUser!!.uid) {
                            mUsers?.add(user)
                        }
                    }
                }

                userAdapter?.notifyDataSetChanged()
            }
        })
    }

    private fun retriveUser() {

        val userRef = FirebaseDatabase.getInstance().reference.child("Users")

        userRef.addValueEventListener(object : ValueEventListener {

            override fun onCancelled(p0: DatabaseError) {
                TODO("Not yet implemented")
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(view?.search_edit_text?.text.toString() == "") {
                    mUsers?.clear()

                    for(snapshot in dataSnapshot.children) {
                        val user = snapshot.getValue(User::class.java)
                        if(user != null) {
                            mUsers?.add(user)
                        }
                    }

                    userAdapter?.notifyDataSetChanged()
                }
            }

        })
    }
}