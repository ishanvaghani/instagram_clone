package com.instagramclone.Notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.instagramclone.CommentsActivity
import com.instagramclone.MessageActivity
import com.instagramclone.PostDetailsActivity
import com.instagramclone.UserProfileActivity

class FirebaseMessaging : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val sented = remoteMessage.data["sented"]
        val user = remoteMessage.data["user"]
        val preferences = getSharedPreferences("PREFS", Context.MODE_PRIVATE)
        val currentUser = preferences.getString("currentuser", "none")
        val firebaseUser = FirebaseAuth.getInstance().currentUser

        if (firebaseUser != null && sented == firebaseUser.uid) {
            if (currentUser != user) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    sendOreoNotification(remoteMessage)
                }
                else {
                    sendNotification(remoteMessage)
                }
            }
        }
    }

    private fun sendOreoNotification(remoteMessage: RemoteMessage) {

        val user = remoteMessage.data["user"]
        val icon = remoteMessage.data["icon"]
        val title = remoteMessage.data["title"]
        val body = remoteMessage.data["body"]
        val postId = remoteMessage.data["postId"]

        val notification = remoteMessage.notification
        val j = user!!.replace("[\\D]".toRegex(), "").toInt()


        if(title.equals("New Message")) {
            val intent = Intent(this, MessageActivity::class.java)

            val bundle = Bundle()
            bundle.putString("userid", user)
            intent.putExtras(bundle)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            val pendingIntent = PendingIntent.getActivity(this, j, intent, PendingIntent.FLAG_ONE_SHOT)
            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val oreoNotification = OreoNotification(this)
            val builder: Notification.Builder = oreoNotification.getOreoNotification(title, body, pendingIntent, defaultSound, icon)

            var i = 0
            if (j > 0) {
                i = j
            }
            oreoNotification.getManager()!!.notify(i, builder.build())
        }

        else if(title.equals("New Like")) {
            val intent = Intent(this, PostDetailsActivity::class.java)

            val bundle = Bundle()
            bundle.putString("postId", postId)
            intent.putExtras(bundle)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            val pendingIntent = PendingIntent.getActivity(this, j, intent, PendingIntent.FLAG_ONE_SHOT)
            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val oreoNotification = OreoNotification(this)
            val builder: Notification.Builder = oreoNotification.getOreoNotification(title, body, pendingIntent, defaultSound, icon)

            var i = 0
            if (j > 0) {
                i = j
            }
            oreoNotification.getManager()!!.notify(i, builder.build())
        }

        else if(title.equals("New Comment")) {
            val intent = Intent(this, CommentsActivity::class.java)

            val bundle = Bundle()
            bundle.putString("postId", postId)
            bundle.putString("PublisherId", user)
            intent.putExtras(bundle)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            val pendingIntent = PendingIntent.getActivity(this, j, intent, PendingIntent.FLAG_ONE_SHOT)
            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val oreoNotification = OreoNotification(this)
            val builder: Notification.Builder = oreoNotification.getOreoNotification(title, body, pendingIntent, defaultSound, icon)

            var i = 0
            if (j > 0) {
                i = j
            }
            oreoNotification.getManager()!!.notify(i, builder.build())
        }

        else if(title.equals("New Follower")) {

            val intent = Intent(this, UserProfileActivity::class.java)
            val pref = getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
            pref.putString("profileId", user)
            pref.apply()
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            val pendingIntent = PendingIntent.getActivity(this, j, intent, PendingIntent.FLAG_ONE_SHOT)
            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val oreoNotification = OreoNotification(this)
            val builder: Notification.Builder = oreoNotification.getOreoNotification(title, body, pendingIntent, defaultSound, icon)

            var i = 0
            if (j > 0) {
                i = j
            }
            oreoNotification.getManager()!!.notify(i, builder.build())
        }

    }

    private fun sendNotification(remoteMessage: RemoteMessage) {
        val user = remoteMessage.data["user"]
        val icon = remoteMessage.data["icon"]
        val title = remoteMessage.data["title"]
        val body = remoteMessage.data["body"]
        val postId = remoteMessage.data["postId"]

        val notification = remoteMessage.notification
        val j = user!!.replace("[\\D]".toRegex(), "").toInt()

        if(title.equals("New Message")) {
            val intent = Intent(this, MessageActivity::class.java)

            val bundle = Bundle()
            bundle.putString("userid", user)
            intent.putExtras(bundle)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            val pendingIntent = PendingIntent.getActivity(this, j, intent, PendingIntent.FLAG_ONE_SHOT)
            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val builder = NotificationCompat.Builder(this)
                .setSmallIcon(icon!!.toInt())
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSound)
                .setContentIntent(pendingIntent)
            val noti = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            var i = 0
            if (j > 0) {
                i = j
            }
            noti.notify(i, builder.build())
        }

        else if(title.equals("New Like")) {
            val intent = Intent(this, PostDetailsActivity::class.java)

            val bundle = Bundle()
            bundle.putString("postId", postId)
            intent.putExtras(bundle)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            val pendingIntent = PendingIntent.getActivity(this, j, intent, PendingIntent.FLAG_ONE_SHOT)
            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val builder = NotificationCompat.Builder(this)
                .setSmallIcon(icon!!.toInt())
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSound)
                .setContentIntent(pendingIntent)
            val noti = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            var i = 0
            if (j > 0) {
                i = j
            }
            noti.notify(i, builder.build())
        }

        else if(title.equals("New Comment")) {
            val intent = Intent(this, CommentsActivity::class.java)

            val bundle = Bundle()
            bundle.putString("postId", postId)
            bundle.putString("PublisherId", user)
            intent.putExtras(bundle)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            val pendingIntent = PendingIntent.getActivity(this, j, intent, PendingIntent.FLAG_ONE_SHOT)
            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val builder = NotificationCompat.Builder(this)
                .setSmallIcon(icon!!.toInt())
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSound)
                .setContentIntent(pendingIntent)
            val noti = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            var i = 0
            if (j > 0) {
                i = j
            }
            noti.notify(i, builder.build())
        }

        else if(title.equals("New Follower")) {
            val intent = Intent(this, UserProfileActivity::class.java)

            val pref = getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
            pref.putString("profileId", user)
            pref.apply()
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            val pendingIntent = PendingIntent.getActivity(this, j, intent, PendingIntent.FLAG_ONE_SHOT)
            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val builder = NotificationCompat.Builder(this)
                .setSmallIcon(icon!!.toInt())
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSound)
                .setContentIntent(pendingIntent)
            val noti = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            var i = 0
            if (j > 0) {
                i = j
            }
            noti.notify(i, builder.build())
        }
    }
}