package com.example.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** Recebe mensagens FCM do mesmo backend de pedidos usado pelo Capacitor. */
class ItaSuperFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PushNotificationManager.registerFcmToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val title = message.notification?.title ?: data["title"] ?: "ItaSuper"
        val body = message.notification?.body ?: data["body"] ?: "Há uma atualização no seu pedido."
        PushNotificationManager.showOrderNotification(applicationContext, title, body, data)
    }
}
