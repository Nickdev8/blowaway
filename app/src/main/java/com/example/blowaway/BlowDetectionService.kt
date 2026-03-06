package com.example.blowaway

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder

class BlowDetectionService : Service() {

    @Volatile
    private var workerThread: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ARM -> {
                isArmed = true
                updateForegroundNotification(isActivelyListening = false)
                return START_STICKY
            }

            ACTION_DISARM -> {
                isArmed = false
                workerThread?.interrupt()
                workerThread = null
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_DETECT -> {
                if (!isArmed) {
                    return START_NOT_STICKY
                }

                val notificationKey = intent.getStringExtra(EXTRA_NOTIFICATION_KEY)
                if (notificationKey.isNullOrBlank()) {
                    return START_STICKY
                }

                if (workerThread?.isAlive == true) {
                    return START_STICKY
                }

                updateForegroundNotification(isActivelyListening = true)

                workerThread = Thread {
                    val detected = BlowDetector().detectBlow(LISTEN_WINDOW_MS)
                    if (detected) {
                        NotificationController.cancelNotification(notificationKey)
                    }
                    if (isArmed) {
                        updateForegroundNotification(isActivelyListening = false)
                    }
                }.apply {
                    name = "BlowDetectionThread"
                    start()
                }

                return START_STICKY
            }

            null -> {
                if (isArmed) {
                    updateForegroundNotification(isActivelyListening = false)
                    return START_STICKY
                }
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        workerThread?.interrupt()
        workerThread = null
        isArmed = false
        super.onDestroy()
    }

    private fun createForegroundNotification(isActivelyListening: Boolean): Notification {
        ensureNotificationChannel()
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.foreground_notification_title))
            .setContentText(
                if (isActivelyListening) {
                    getString(R.string.foreground_notification_active_text)
                } else {
                    getString(R.string.foreground_notification_idle_text)
                }
            )
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    private fun updateForegroundNotification(isActivelyListening: Boolean) {
        val notification = createForegroundNotification(isActivelyListening)
        startForeground(
            FOREGROUND_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )
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
        const val ACTION_ARM = "com.example.blowaway.action.ARM"
        const val ACTION_DETECT = "com.example.blowaway.action.DETECT"
        const val ACTION_DISARM = "com.example.blowaway.action.DISARM"
        const val EXTRA_NOTIFICATION_KEY = "notification_key"
        const val CHANNEL_ID = "blow_detection"

        @Volatile
        private var isArmed = false

        private const val FOREGROUND_NOTIFICATION_ID = 2001
        private const val LISTEN_WINDOW_MS = 8_000L

        fun createStartIntent(context: android.content.Context): Intent {
            return Intent(context, BlowDetectionService::class.java).setAction(ACTION_ARM)
        }

        fun createStopIntent(context: android.content.Context): Intent {
            return Intent(context, BlowDetectionService::class.java).setAction(ACTION_DISARM)
        }

        fun createDetectIntent(
            context: android.content.Context,
            notificationKey: String
        ): Intent {
            return Intent(context, BlowDetectionService::class.java)
                .setAction(ACTION_DETECT)
                .putExtra(EXTRA_NOTIFICATION_KEY, notificationKey)
        }

        fun isArmed(): Boolean = isArmed
    }
}
