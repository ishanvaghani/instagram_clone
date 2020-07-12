package com.instagramclone

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.instagramclone.ImportantClasses.UserStatus
import com.theartofdev.edmodo.cropper.CropImage
import id.zelory.compressor.Compressor
import kotlinx.android.synthetic.main.activity_add_post.*
import kotlinx.android.synthetic.main.activity_add_story.*
import java.io.ByteArrayOutputStream
import java.io.File

class AddStoryActivity : AppCompatActivity() {

    private var myUrl = ""
    private var imageUri: Uri? = null
    private var storageStoryRef: StorageReference? = null
    var finalImage: ByteArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        setContentView(R.layout.activity_add_story)

        storageStoryRef = FirebaseStorage.getInstance().reference.child("Story Pictures")

        save_new_story_btn.setOnClickListener {
            uploadStory()
        }

        close_add_story.setOnClickListener {
            finish()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
            }
            else {
                CropImage.activity().start(this)
            }
        }
        else {
            CropImage.activity().start(this)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val result = CropImage.getActivityResult(data)
            imageUri = result.uri
            image_story.setImageURI(imageUri)

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

    private fun uploadStory() {

        when {
            imageUri == null -> Toast.makeText(this, "Choose image", Toast.LENGTH_SHORT).show()

            else -> {

                val storyRef = FirebaseDatabase.getInstance().reference.child("Story").child(FirebaseAuth.getInstance().currentUser!!.uid)

                val progressDialog = ProgressDialog(this)
                progressDialog.setTitle("Uploading Story")
                progressDialog.setMessage("Please wait...")
                progressDialog.setCanceledOnTouchOutside(false)
                progressDialog.show()

                val fileref = storageStoryRef!!.child(System.currentTimeMillis().toString()+".jpg")
                val storyId = storyRef.push().key.toString()

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

                        val timeEnd = System.currentTimeMillis() + 86400000 //one day

                        val storyMap = HashMap<String, Any>()
                        storyMap["userid"] = FirebaseAuth.getInstance().currentUser!!.uid
                        storyMap["imageurl"] = myUrl
                        storyMap["timestart"] = ServerValue.TIMESTAMP
                        storyMap["timeend"] = timeEnd
                        storyMap["storyid"] = storyId
                        storyRef.child(storyId).setValue(storyMap)

                        Toast.makeText(this, "Story uploaded", Toast.LENGTH_SHORT).show()

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

    override fun onResume() {
        super.onResume()
        UserStatus.online()
    }

    override fun onPause() {
        super.onPause()
        UserStatus.offline()
    }
}