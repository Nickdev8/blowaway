package com.example.blowaway

object NotificationController {

    @Volatile
    private var listener: BlowNotificationListener? = null

    fun register(listenerService: BlowNotificationListener) {
        listener = listenerService
    }

    fun unregister(listenerService: BlowNotificationListener) {
        if (listener === listenerService) {
            listener = null
        }
    }

    fun cancelNotification(notificationKey: String) {
        listener?.cancelNotification(notificationKey)
    }
}
