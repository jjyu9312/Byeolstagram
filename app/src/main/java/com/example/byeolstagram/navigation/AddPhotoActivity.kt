package com.example.byeolstagram.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.byeolstagram.R
import com.example.byeolstagram.navigation.model.ContentDTO
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_add_photo.*
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivity : AppCompatActivity() {

    var PICK_IMAGE_FROM_ALBUM = 0;

    var storage : FirebaseStorage? = null

    var photoUri : Uri? = null

    var auth : FirebaseAuth? = null

    var firestore : FirebaseFirestore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo)

        // Initiate storage
        storage   = FirebaseStorage.getInstance()
        auth      = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // open the album
        var photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM)

        // add image upload event
        addphoto_btn_upload.setOnClickListener {
            contentUpload()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_FROM_ALBUM) {
            if (resultCode == Activity.RESULT_OK) {

                // This is path to selected image
                photoUri = data?.data
                addphoto_image.setImageURI(photoUri)
            } else {

                // 취소 버튼 눌렀을 때 작동
                finish()
            }
        }
    }

    fun contentUpload() {

        // Make finename
        var  timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        var imageFileName = "IMAGE_" + timestamp + "_.png";
        var storageRef = storage?.reference?.child("images")?.child(imageFileName)

        // File Upload

        // Promise method <- 구글에서 권장하는 방식
        storageRef?.putFile(photoUri!!)?.continueWithTask {
            task : Task<UploadTask.TaskSnapshot> ->
                return@continueWithTask storageRef.downloadUrl
        }?.addOnSuccessListener { uri ->
            var contentDTO = ContentDTO()

            //Insert downloadurl of iamge
            contentDTO.imageUrl = uri.toString()

            // Insert uid of suer
            contentDTO.uid = auth?.currentUser?.uid

            // Insert userId
            contentDTO.userId = auth?.currentUser?.email

            // Insert explain of content
            contentDTO.explain = addphoto_edit_explain.text.toString()

            // Insert timestamp
            contentDTO.timestamp = System.currentTimeMillis()

            // contentDTO를 images 컬렉션 안에다가 데이터를 넣어줌
            firestore?.collection("images")?.document()?.set(contentDTO)

            setResult(Activity.RESULT_OK)

            finish()
        }


        // Callback method
        storageRef?.putFile(photoUri!!)?.addOnSuccessListener {
            /*
            // Toast.makeText(this, getString(R.string.upload_success), Toast.LENGTH_LONG).show()
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                var contentDTO = ContentDTO()

                //Insert downloadurl of iamge
                contentDTO.imageUrl = uri.toString()

                // Insert uid of suer
                contentDTO.uid = auth?.currentUser?.uid

                // Insert userId
                contentDTO.userId = auth?.currentUser?.email

                // Insert explain of content
                contentDTO.explain = addphoto_edit_explain.text.toString()

                // Insert timestamp
                contentDTO.timestamp = System.currentTimeMillis()

                // contentDTO를 images 컬렉션 안에다가 데이터를 넣어줌
                firestore?.collection("images")?.document()?.set(contentDTO)

                setResult(Activity.RESULT_OK)

                finish()
            }
             */
        }
    }
}