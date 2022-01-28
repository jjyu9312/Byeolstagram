package com.example.byeolstagram.navigation.model

data class FollowDTO (
    var followerCount : Int = 0,
    var follwers : MutableMap<String, Boolean> = HashMap(),

    var followingCount : Int = 0,
    var followings : MutableMap<String, Boolean> = HashMap()
)