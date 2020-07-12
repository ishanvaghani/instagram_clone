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
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.instagramclone.Model.User
import com.instagramclone.ImportantClasses.UserStatus
import com.squareup.picasso.Picasso
import com.theartofdev.edmodo.cropper.CropImage
import de.hdodenhof.circleimageview.CircleImageView
import id.zelory.compressor.Compressor
import kotlinx.android.synthetic.main.activity_account_setting.*
import kotlinx.android.synthetic.main.activity_account_setting.bio_profile_frag
import kotlinx.android.synthetic.main.activity_account_setting.full_name_profile_frag
import kotlinx.android.synthetic.main.activity_sign_up.*
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.collections.HashMap

class AccountSettingActivity : AppCompatActivity() {

    private lateinit var firebaseUser: FirebaseUser
    private var checker = ""
    private var myUrl = ""
    private var imageUri: Uri? = null
    private var storageProfilePicref: StorageReference? = null
    var finalImage: ByteArray? = null
    var profile_image: CircleImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        setContentView(R.layout.activity_account_setting)

        firebaseUser = FirebaseAuth.getInstance().currentUser!!
        storageProfilePicref = FirebaseStorage.getInstance().reference.child("Profile Pictures")

        profile_image = findViewById(R.id.profile_image_view_profile_frag)

        logout_btn_profile_frag.setOnClickListener {

            FirebaseAuth.getInstance().signOut()

            UserStatus.offline()

            val intent = (Intent(this, SignInActivity::class.java))
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        close_profile_btn.setOnClickListener {
            finish()
        }

        profile_image!!.setOnClickListener {
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

        save_info_profile_btn.setOnClickListener {
            if(checker == "clicked") {
                updateImageAndInfo()
            }
            else {
                updateUserInfoOnly()
            }
        }

        userInfo()
    }

    private fun chooseImage() {
        CropImage.activity()
            .setAspectRatio(1,1)
            .start(this)
    }

    private fun updateImageAndInfo() {

        val userRef = FirebaseDatabase.getInstance().reference.child("Users")

        val fullname = full_name_profile_frag.text.toString().trim()
        val username = username_profile_frag.text.toString().trim()
        val bio = bio_profile_frag.text.toString().trim()

        when {
            TextUtils.isEmpty(fullname) -> fullname_signup.setError("Field is empty")
            TextUtils.isEmpty(username) -> username_signup.setError("Field is empty")
            TextUtils.isEmpty(bio) -> email_signup.setError("Field is empty")
            imageUri == null -> Toast.makeText(this, "Choose image", Toast.LENGTH_SHORT).show()

            else -> {

                val progressDialog = ProgressDialog(this)
                progressDialog.setTitle("Profile Updating")
                progressDialog.setMessage("Please wait...")
                progressDialog.setCanceledOnTouchOutside(false)
                progressDialog.show()

                val fileref = storageProfilePicref!!.child(firebaseUser.uid+".jpg")

                val uploadTask: StorageTask<*>
                uploadTask = fileref.putFile(imageUri!!)

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

                        val userMap = HashMap<String, Any>()
                        userMap["fullname"] = full_name_profile_frag.text.toString().toLowerCase()
                        userMap["username"] = username_profile_frag.text.toString().toLowerCase()
                        userMap["bio"] = bio_profile_frag.text.toString()
                        userMap["image"] = myUrl
                        userRef.child(firebaseUser.uid).updateChildren(userMap)

                        Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode ==Activity.RESULT_OK && data != null) {
            val result = CropImage.getActivityResult(data)
            imageUri = result.uri
            profile_image!!.setImageURI(imageUri)
            checker = "clicked"

            val actualfile = File(imageUri!!.path)
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
    }

    private fun updateUserInfoOnly() {
        val userRef = FirebaseDatabase.getInstance().reference.child("Users")

        val fullname = full_name_profile_frag.text.toString().trim()
        val username = username_profile_frag.text.toString().trim()
        val bio = bio_profile_frag.text.toString().trim()

        when {
            TextUtils.isEmpty(fullname) -> fullname_signup.setError("Field is empty")
            TextUtils.isEmpty(username) -> username_signup.setError("Field is empty")
            TextUtils.isEmpty(bio) -> email_signup.setError("Field is empty")

            else -> {
                val userMap = HashMap<String, Any>()
                userMap["fullname"] = full_name_profile_frag.text.toString()
                userMap["username"] = username_profile_frag.text.toString()
                userMap["search"] = username_profile_frag.text.toString().toLowerCase()
                userMap["bio"] = bio_profile_frag.text.toString()
                userRef.child(firebaseUser.uid).updateChildren(userMap)

                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()

                finish()
            }
        }
    }

    private fun userInfo() {
        val userRef = FirebaseDatabase.getInstance().reference.child("Users").child(firebaseUser.uid)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if(p0.exists()) {
                    val user = p0.getValue<User>(User::class.java)
                    Picasso.get().load(user!!.getImage()).placeholder(R.drawable.profile).into(profile_image)
                    username_profile_frag.setText(user.getUsername())
                    bio_profile_frag.setText(user.getBio())
                    full_name_profile_frag.setText(user.getFullname())
                }
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