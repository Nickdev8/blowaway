package com.example.blowaway

import android.app.NotificationManager
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class BlowNotificationListener : NotificationListenerService() {

    override fun onCreate() {
        super.onCreate()
        NotificationController.register(this)
    }

    override fun onDestroy() {
        NotificationController.unregister(this)
        super.onDestroy()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationController.register(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (shouldIgnoreNotification(sbn)) {
            return
        }

        if (!BlowDetectionService.isArmed()) {
            return
        }

        if (!sbn.isClearable) {
            return
        }

        if (!isHeadsUpCandidate(sbn.key)) {
            return
        }

        try {
            startService(BlowDetectionService.createDetectIntent(this, sbn.key))
        } catch (exception: IllegalStateException) {
            Log.w(TAG, "Android blocked the foreground service start for blow detection.", exception)
        } catch (exception: SecurityException) {
            Log.w(TAG, "Missing runtime conditions for microphone foreground service.", exception)
        }
    }

    private fun isHeadsUpCandidate(notificationKey: String): Boolean {
        val ranking = Ranking()
        val hasRanking = currentRanking.getRanking(notificationKey, ranking)
        return hasRanking && ranking.importance >= NotificationManager.IMPORTANCE_HIGH
    }

    private fun shouldIgnoreNotification(sbn: StatusBarNotification): Boolean {
        return sbn.packageName == packageName &&
            sbn.notification.channelId == BlowDetectionService.CHANNEL_ID
    }

    companion object {
        private const val TAG = "BlowNotificationListener"
    }
}
