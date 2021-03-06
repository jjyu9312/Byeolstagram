package com.example.byeolstagram

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.byeolstagram.navigation.*
import com.example.byeolstagram.util.FcmPush
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bottom_navigation.setOnNavigationItemSelectedListener (this)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)

        // Set default screen
        bottom_navigation.selectedItemId = R.id.action_home

        // token 저장 실행
        registerPushToken()
    }

    /*
    override fun onStop() {
        super.onStop()
        FcmPush.instance.sendMessage("Uid 값", "title", "message")
    }
     */

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UserFragment.PICK_PROFILE_FROW_ALBUM && resultCode == RESULT_OK) {
            var imageUri = data?.data
            var uid = FirebaseAuth.getInstance().currentUser?.uid
            var storageRef = FirebaseStorage.getInstance().reference.child("userProfileImages").child(uid!!) // uid가 파일명
            storageRef.putFile(imageUri!!).continueWithTask { task : Task<UploadTask.TaskSnapshot> ->
                return@continueWithTask storageRef.downloadUrl
            }.addOnSuccessListener {uri ->
                var map = HashMap<String, Any>()
                map["image"] = uri.toString()
                FirebaseFirestore.getInstance().collection("profileImages").document(uid).set(map)
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        setToolbarDefault()
        
        when(item.itemId) {
            R.id.action_home -> {
                var detailViewFragment = DetailViewFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, detailViewFragment).commit()
                return true
            }
            R.id.action_search -> {
                var gridFragment = GridFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, gridFragment).commit()
                return true
            }
            R.id.action_add_photo -> {

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    startActivity(Intent(this, AddPhotoActivity::class.java))
                }
                return true
            }
            R.id.action_favorite_alarm -> {
                var alarmFragment = AlarmFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, alarmFragment).commit()
                return true
            }
            R.id.action_account -> {
                var userFragment = UserFragment()
                var bundle = Bundle()
                var uid = FirebaseAuth.getInstance().currentUser?.uid
                bundle.putString("destinationId", uid)
                userFragment.arguments = bundle
                supportFragmentManager.beginTransaction().replace(R.id.main_content, userFragment).commit()
                return true
            }
        }

        return false
    }

    fun setToolbarDefault() {
        toolbar_username.visibility = View.GONE
        toolbar_btn_back.visibility = View.GONE
        toolbar_title_image.visibility = View.VISIBLE
    }

    // 특정 디바이스에만 푸시 메시지 전송 (default는 전체 전송)
    fun registerPushToken() {
        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener { task ->
            val token = task.result?.token
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            var map = mutableMapOf<String, Any>()
            map["pushToken"] = token!!

            // Firebase에 pushtokens 컬렉션을 만들어 token 저장
            FirebaseFirestore.getInstance().collection("pushtokens").document(uid!!).set(map)
        }
    }
}