package com.example.blowaway

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder

class BlowDetectionService : Service() {

    @Volatile
    private var workerThread: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationKey = intent?.getStringExtra(EXTRA_NOTIFICATION_KEY)
        if (notificationKey.isNullOrBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(FOREGROUND_NOTIFICATION_ID, createForegroundNotification())

        if (workerThread?.isAlive == true) {
            return START_NOT_STICKY
        }

        workerThread = Thread {
            val detected = BlowDetector().detectBlow(LISTEN_WINDOW_MS)
            if (detected) {
                NotificationController.cancelNotification(notificationKey)
            }
            stopSelf()
        }.apply {
            name = "BlowDetectionThread"
            start()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        workerThread?.interrupt()
        workerThread = null
        super.onDestroy()
    }

    private fun createForegroundNotification(): Notification {
        ensureNotificationChannel()
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.foreground_notification_title))
            .setContentText(getString(R.string.foreground_notification_text))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.foreground_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.foreground_channel_description)
        }

        manager.createNotificationChannel(channel)
    }

    companion object {
        const val EXTRA_NOTIFICATION_KEY = "notification_key"
        const val CHANNEL_ID = "blow_detection"

        private const val FOREGROUND_NOTIFICATION_ID = 2001
        private const val LISTEN_WINDOW_MS = 8_000L
    }
}
