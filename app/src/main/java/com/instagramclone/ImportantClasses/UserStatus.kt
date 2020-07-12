package com.instagramclone.ImportantClasses

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

object UserStatus {

    private fun status(status: String) {

        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            val reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.uid)
            val hashMap = HashMap<String, Any>()
            hashMap["status"] = status
            reference.updateChildren(hashMap)
        }
    }

    fun offline() {
        val savetime: String
        val savedate: String
        val main: String

        val calendar = Calendar.getInstance()
        val currentDate = SimpleDateFormat("dd MMM yyyy")
        savedate = currentDate.format(calendar.time)
        val currentTime = SimpleDateFormat("hh:mm a")
        savetime = currentTime.format(calendar.time)
        main = "$savetime, $savedate"
        status(main)
    }

    fun online() {
        status("online")
    }
}