package com.instagramclone

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.instagramclone.Model.Story
import com.instagramclone.Model.User
import com.instagramclone.ImportantClasses.UserStatus
import com.squareup.picasso.Picasso
import jp.shts.android.storiesprogressview.StoriesProgressView
import kotlinx.android.synthetic.main.activity_story.*

class StoryActivity : AppCompatActivity() {

    var currentUserId: String = ""
    var userId: String = ""
    var counter = 0

    var pressTime = 0L
    var limit = 500L

    var imagesList: List<String>? = null
    var storyIds: List<String>? = null

    var storiesProcessView: StoriesProgressView? = null

    private var story_image: ImageView? = null
    private var story_delete: TextView? = null
    private var story_layout_seen: LinearLayout? = null

    private val onTouchListener =  View.OnTouchListener{ view, motionEvent ->

        when(motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                pressTime = System.currentTimeMillis()
                storiesProcessView!!.pause()
                return@OnTouchListener false
            }
            MotionEvent.ACTION_UP -> {
                val now = System.currentTimeMillis()
                storiesProcessView!!.resume()
                return@OnTouchListener limit < now - pressTime
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        setContentView(R.layout.activity_story)

        story_image = findViewById(R.id.image_story)
        story_delete = findViewById(R.id.story_delete)
        story_layout_seen = findViewById(R.id.layout_seen)

        storiesProcessView = findViewById(R.id.stories_progress)

        currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
        userId = intent.getStringExtra("userId")

        story_layout_seen!!.visibility = View.GONE
        story_delete!!.visibility = View.GONE

        if(userId == currentUserId) {
            story_layout_seen!!.visibility = View.VISIBLE
            story_delete!!.visibility = View.VISIBLE
        }

        story_delete!!.setOnClickListener {
            FirebaseDatabase.getInstance().reference.child("Story")
                .child(userId).child(storyIds!![counter]).removeValue()
            finish()
        }

        getStories(userId)
        userInfo(userId)

        val reverse: View = findViewById(R.id.reverse)
        reverse.setOnClickListener {
            storiesProcessView!!.reverse()
        }
        reverse.setOnTouchListener(onTouchListener)

        val skip: View = findViewById(R.id.skip)
        skip.setOnClickListener {
            storiesProcessView!!.skip()
        }
        skip.setOnTouchListener(onTouchListener)

        layout_seen.setOnClickListener {
            val intent = Intent(this, ShowUsersActivity::class.java)
            intent.putExtra("id", userId)
            intent.putExtra("storyid", storyIds!![counter])
            intent.putExtra("title", "views")
            startActivity(intent)
        }
    }

    private fun addViewToStory(storyId: String) {

        FirebaseDatabase.getInstance().reference.child("Story").child(userId).child(storyId)
            .child("views").child(currentUserId).setValue(true)

    }

    private fun seenNumber(storyId: String) {

        val ref = FirebaseDatabase.getInstance().reference.child("Story").child(userId).child(storyId).child("views")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                seen_number.text = ""+p0.childrenCount
            }

        })
    }

    private fun userInfo(userId: String) {
        val userRef = FirebaseDatabase.getInstance().reference.child("Users").child(userId)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    val user = p0.getValue<User>(User::class.java)
                    Picasso.get().load(user!!.getImage()).placeholder(R.drawable.profile)
                        .into(story_profile_image)
                    story_username.text = user.getUsername()
                }
            }
        })
    }

    private fun getStories(userId: String) {
        imagesList = ArrayList()
        storyIds = ArrayList()

        val ref = FirebaseDatabase.getInstance().reference.child("Story").child(userId)

        ref.addValueEventListener(object : ValueEventListener, StoriesProgressView.StoriesListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {

                if(p0.exists()) {
                    (imagesList as ArrayList<String>).clear()
                    (storyIds as ArrayList<String>).clear()

                    for(snapshot in p0.children) {

                        val story = snapshot.getValue(Story::class.java)
                        val timeCurrent = System.currentTimeMillis()

                        if(timeCurrent>story!!.getTimeStart() && timeCurrent<story.getTimeEnd()) {

                            (imagesList as ArrayList<String>).add(story.getImageUrl())
                            (storyIds as ArrayList<String>).add(story.getStoryId())

                        }
                    }
                    storiesProcessView!!.setStoriesCount((imagesList as ArrayList<String>).size)
                    storiesProcessView!!.setStoryDuration(6000)
                    storiesProcessView!!.setStoriesListener(this)
                    Picasso.get().load(imagesList!!.get(counter)).placeholder(R.drawable.profile).into(story_image)
                    storiesProcessView!!.startStories(counter)
                    addViewToStory((storyIds!!.get(counter)))
                    seenNumber((storyIds!!.get(counter)))
                }
            }

            override fun onComplete() {
                finish()
            }

            override fun onPrev() {

                if(counter - 1 < 0) return
                Picasso.get().load(imagesList!![--counter]).placeholder(R.drawable.profile).into(image_story)
                seenNumber((storyIds!![counter]))
            }

            override fun onNext() {
                Picasso.get().load(imagesList!![++counter]).placeholder(R.drawable.profile).into(image_story)
                addViewToStory((storyIds!![counter]))
                seenNumber((storyIds!![counter]))
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        storiesProcessView!!.destroy()
    }

    override fun onResume() {
        super.onResume()
        storiesProcessView!!.resume()
        UserStatus.online()
    }

    override fun onPause() {
        super.onPause()
        storiesProcessView!!.pause()
        UserStatus.offline()
    }
}