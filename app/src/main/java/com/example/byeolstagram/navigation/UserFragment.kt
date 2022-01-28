package com.example.byeolstagram.navigation

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.byeolstagram.LoginActivity
import com.example.byeolstagram.MainActivity
import com.example.byeolstagram.R
import com.example.byeolstagram.navigation.model.ContentDTO
import com.example.byeolstagram.navigation.model.FollowDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.view.*

class UserFragment : Fragment() {

    var fragementView : View? = null
    var firestore : FirebaseFirestore? = null
    var uid : String? = null
    var auth : FirebaseAuth? = null
    var currentUserUid : String? = null

    companion object {
        var PICK_PROFILE_FROW_ALBUM = 10
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragementView = LayoutInflater.from(activity).inflate(R.layout.fragment_user, container, false)
        uid = arguments?.getString("destinationUid")
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid // 내 게정의 정보와 상대방의 계정 정보를 비교하기 위함.

        if (uid == currentUserUid) {
            // myPage
            fragementView?.account_btn_follow_signout?.text = getString(R.string.signout)
            fragementView?.account_btn_follow_signout?.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity, LoginActivity::class.java))
                auth?.signOut()
            }
        } else {
            // otherPage
            fragementView?.account_btn_follow_signout?.text = getString(R.string.follow)
            var mainactivity = (activity as MainActivity)
            mainactivity?.toolbar_username?.text = arguments?.getString("userId")
            mainactivity?.toolbar_btn_back.setOnClickListener {
                mainactivity.bottom_navigation.selectedItemId = R.id.action_home
            }
            mainactivity.toolbar_title_image?.visibility = View.GONE
            mainactivity?.toolbar_username?.visibility = View.VISIBLE
            mainactivity?.toolbar_btn_back?.visibility = View.VISIBLE

            fragementView?.account_btn_follow_signout?.setOnClickListener {
                requestFollow()
            }

        }

        fragementView?.account_recyclerview?.adapter = UserFragementRecyclerViewAdapter()
        fragementView?.account_recyclerview?.layoutManager = GridLayoutManager(activity!!, 3)

        fragementView?.account_iv_profile?.setOnClickListener {
            var photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            activity?.startActivityForResult(photoPickerIntent, PICK_PROFILE_FROW_ALBUM)
        }
        getProfileImage()
        getFollowerAndFollowing()
        return fragementView
    }

    fun getFollowerAndFollowing() {
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if (documentSnapshot == null) return@addSnapshotListener
            var followDTO = documentSnapshot.toObject(FollowDTO::class.java)
            if (followDTO?.followingCount != null) {
                fragementView?.account_tv_following_count?.text = followDTO?.followingCount?.toString()
            }
            if (followDTO?.followerCount != null) {
                fragementView?.account_tv_follow_count?.text = followDTO?.followerCount?.toString()
                if (followDTO?.follwers?.containsKey(currentUserUid!!)) {
                    fragementView?.account_btn_follow_signout?.text = getString(R.string.follow_cancel)
                    fragementView?.account_btn_follow_signout?.background?.setColorFilter(ContextCompat.getColor(activity!!, R.color.colorLightGray), PorterDuff.Mode.MULTIPLY)
                } else {
                    if (uid != currentUserUid) {
                        fragementView?.account_btn_follow_signout?.text = getString(R.string.follow)
                        fragementView?.account_btn_follow_signout?.background?.colorFilter = null
                    }
                }
            }
        }
    }

    fun requestFollow() {
        // Save data to my account
        var tsDocFollowing = firestore?.collection("users")?.document(currentUserUid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)
            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO!!.followingCount = 1
                followDTO!!.follwers[uid!!] = true

                transaction.set(tsDocFollowing, followDTO) // 데이터가 DB에 담기게 됨.
                return@runTransaction
            }

            if (followDTO.followings.containsKey(uid)) {
                // It remove following third person when a third person follow me
                followDTO.followingCount = followDTO?.followingCount - 1
                followDTO?.follwers?.remove(uid)
            } else {
                // It add following third person when a third person do not follow me
                followDTO.followingCount = followDTO?.followingCount + 1
                followDTO?.follwers[uid!!] = true
            }

            transaction.set(tsDocFollowing, followDTO)
            return@runTransaction
        }

        // Save data to third person
        var tsDocFollower = firestore?.collection("users")?.document(uid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.follwers[currentUserUid!!] = true

                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }

            if (followDTO!!.follwers.containsKey(currentUserUid)) {
                //It remove my follower when I follow a third person
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.follwers.remove(currentUserUid!!)
                // It add my follower when I don't follow a third person
                followDTO!!.followerCount = followDTO!!.followerCount + 1
                followDTO!!.follwers[currentUserUid!!] = true
            }

            transaction.set(tsDocFollower, followDTO!!)
            return@runTransaction
        }
    }

    fun getProfileImage() {
        firestore?.collection("profileImage")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if (documentSnapshot == null) return@addSnapshotListener
            if (documentSnapshot.data != null) {
                var url = documentSnapshot?.data!!["image"]
                Glide.with(activity!!).load(url).apply(RequestOptions().circleCrop()).into(fragementView?.account_iv_profile!!)
            }
        }
    }

    inner class UserFragementRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        init {
            firestore?.collection("images")?.whereEqualTo("uid", uid)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                // Sometimes, This code return null of querySnapshot when it signout
                if (querySnapshot == null) return@addSnapshotListener

                // Get data
                for (snapshot in querySnapshot.documents) {
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }

                fragementView?.account_tv_post_count?.text = contentDTOs.size.toString()
                notifyDataSetChanged() // 새로고침 될 수 있게 함
            }
        }

        override fun onCreateViewHolder(p0 : ViewGroup, p1 : Int): RecyclerView.ViewHolder {
            var width = resources.displayMetrics.widthPixels / 3
            var imageView = ImageView(p0.context)

            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width, width)

            return CustomViewHolder(imageView)
        }

        inner class CustomViewHolder(var imageView : ImageView) : RecyclerView.ViewHolder(imageView) {
        }

        override fun onBindViewHolder(p0 : RecyclerView.ViewHolder, p1 : Int) {
            var imageView = (p0 as CustomViewHolder).imageView
            Glide.with(p0.itemView.context).load(contentDTOs[p1].imageUrl).apply(RequestOptions().centerCrop()).into(imageView)
        }

        override fun getItemCount(): Int {

            return contentDTOs.size
        }

    }
}