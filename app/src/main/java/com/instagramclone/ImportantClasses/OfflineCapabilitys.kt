package com.instagramclone.ImportantClasses

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

class OfflineCapabilitys : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
}