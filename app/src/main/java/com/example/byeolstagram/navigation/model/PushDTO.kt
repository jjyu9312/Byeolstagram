package com.example.byeolstagram.navigation.model


data class PushDTO (
    var to : String? = null,
    var notificatoion : Notification = Notification()
) {
    data class Notification(
        var body : String? = null,
        var title : String? = null
    )
}