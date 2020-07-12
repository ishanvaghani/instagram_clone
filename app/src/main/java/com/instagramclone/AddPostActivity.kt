package com.instagramclone

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.instagramclone.ImportantClasses.UserStatus
import com.instagramclone.Model.Post
import com.squareup.picasso.Picasso
import com.theartofdev.edmodo.cropper.CropImage
import id.zelory.compressor.Compressor
import kotlinx.android.synthetic.main.activity_add_post.*
import java.io.ByteArrayOutputStream
import java.io.File

class AddPostActivity : AppCompatActivity() {

    private var myUrl = ""
    private var imageUri: Uri? = null
    private var storagePostPicRef: StorageReference? = null
    var finalImage: ByteArray? = null
    var postId: String? = null
    var post_image: ImageView? = null
    var title: TextView? = null
    var description: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        setContentView(R.layout.activity_add_post)

        title = findViewById(R.id.add_post_title)
        post_image = findViewById(R.id.image_post)
        description = findViewById(R.id.description_post)

        storagePostPicRef = FirebaseStorage.getInstance().reference.child("Post Pictures")

        val intent = intent
        if(intent != null) {
            postId = intent.getStringExtra("postId")
        }

        if(postId != null) {
            loadEditPost()
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
                }
                else {
                    chooseImage()
                }
            }
            else {
                chooseImage()
            }
        }

        save_new_post_btn.setOnClickListener {
            if(postId == null) {
                uploadPost()
            }
            else {
                editUploadPost()
            }
        }

        close_add_post.setOnClickListener {
            finish()
        }
    }

    private fun chooseImage() {
        CropImage.activity()
            .setAspectRatio(1,1)
            .start(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val result = CropImage.getActivityResult(data)
            imageUri = result.uri
            post_image!!.setImageURI(imageUri)

            val actualfile = File(imageUri!!.getPath())
            try {
                val compressedImage: Bitmap = Compressor(this)
                    .setQuality(10)
                    .compressToBitmap(actualfile)
                val byteArrayOutputStream = ByteArrayOutputStream()
                compressedImage.compress(Bitmap.CompressFormat.JPEG, 10, byteArrayOutputStream)
                finalImage = byteArrayOutputStream.toByteArray()
            } catch (e: Exception) {
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
        }
        else {
            finish()
        }
    }

    private fun loadEditPost() {
        title!!.text = "Edit Post"
        val postRef = FirebaseDatabase.getInstance().reference.child("Posts").child(postId!!)

        postRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if(p0.exists()) {
                    val post = p0.getValue(Post::class.java)
                    Picasso.get().load(post!!.getImage()).placeholder(R.drawable.profile).into(post_image)
                    description!!.text = post.getDescription()
                }
            }
        })
    }

    private fun uploadPost() {

        val description = description!!.text.toString().trim()

        when {
            TextUtils.isEmpty(description) -> description_post.error = "Field is empty"
            imageUri == null -> Toast.makeText(this, "Choose image", Toast.LENGTH_SHORT).show()

            else -> {

                val postRef = FirebaseDatabase.getInstance().reference.child("Posts")

                val progressDialog = ProgressDialog(this)
                progressDialog.setTitle("Uploading Post")
                progressDialog.setMessage("Please wait...")
                progressDialog.setCanceledOnTouchOutside(false)
                progressDialog.show()

                val fileref = storagePostPicRef!!.child(System.currentTimeMillis().toString()+".jpg")
                val postId = postRef.push().key

                val uploadTask: StorageTask<*>
                uploadTask = fileref.putBytes(finalImage!!)

                uploadTask.continueWithTask(Continuation <UploadTask.TaskSnapshot, Task<Uri>> { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let {
                            progressDialog.dismiss()
                            throw it
                        }
                    }
                    return@Continuation fileref.downloadUrl
                }).addOnCompleteListener { task->
                    if(task.isSuccessful) {
                        val downloadUrl = task.result
                        myUrl = downloadUrl.toString()

                        val postMap = HashMap<String, Any>()
                        postMap["postId"] = postId!!
                        postMap["image"] = myUrl
                        postMap["description"] = description
                        postMap["publisher"] = FirebaseAuth.getInstance().currentUser!!.uid
                        postRef.child(postId).setValue(postMap)

                        Toast.makeText(this, "Post uploaded", Toast.LENGTH_SHORT).show()

                        val intent = (Intent(this, MainActivity::class.java))
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        finish()
                        progressDialog.dismiss()
                    }
                    else {
                        progressDialog.dismiss()
                    }
                }
            }
        }
    }

    private fun editUploadPost() {

        val description = description!!.text.toString().trim()

        when {
            TextUtils.isEmpty(description) -> description_post.error = "Field is empty"

            else -> {

                val postRef = FirebaseDatabase.getInstance().reference.child("Posts")
                val postMap = HashMap<String, Any>()
                postMap["description"] = description
                postRef.child(postId!!).updateChildren(postMap).addOnCompleteListener {
                    Toast.makeText(this, "Post updated", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
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