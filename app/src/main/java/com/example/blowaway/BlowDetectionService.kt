package com.example.blowaway

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log

class BlowDetectionService : Service() {

    @Volatile
    private var workerThread: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ARM -> {
                return if (armForegroundIfPossible()) {
                    START_STICKY
                } else {
                    START_NOT_STICKY
                }
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

                workerThread = Thread {
                    val detected = BlowDetector().detectBlow(LISTEN_WINDOW_MS)
                    if (detected) {
                        NotificationController.cancelNotification(notificationKey)
                    }
                }.apply {
                    name = "BlowDetectionThread"
                    start()
                }

                return START_STICKY
            }

            null -> {
                if (armForegroundIfPossible()) {
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

    private fun createForegroundNotification(): Notification {
        ensureNotificationChannel(this)
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.foreground_notification_title))
            .setContentText(getString(R.string.foreground_notification_idle_text))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setLocalOnly(true)
            .build()
    }

    private fun ensureForegroundNotification() {
        val notification = createForegroundNotification()
        startForeground(
            FOREGROUND_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )
    }

    private fun armForegroundIfPossible(): Boolean {
        if (!BackgroundModeController.hasRequiredSetup(this)) {
            isArmed = false
            stopSelf()
            return false
        }

        return try {
            ensureForegroundNotification()
            isArmed = true
            true
        } catch (exception: IllegalStateException) {
            Log.w(TAG, "Android blocked the microphone foreground service start.", exception)
            isArmed = false
            stopSelf()
            false
        } catch (exception: SecurityException) {
            Log.w(TAG, "Foreground microphone service permissions are not currently eligible.", exception)
            isArmed = false
            stopSelf()
            false
        }
    }

    companion object {
        const val ACTION_ARM = "com.example.blowaway.action.ARM"
        const val ACTION_DETECT = "com.example.blowaway.action.DETECT"
        const val ACTION_DISARM = "com.example.blowaway.action.DISARM"
        const val EXTRA_NOTIFICATION_KEY = "notification_key"
        const val CHANNEL_ID = "blow_detection"

        private const val TAG = "BlowDetectionService"
        @Volatile
        private var isArmed = false

        private const val FOREGROUND_NOTIFICATION_ID = 2001
        private const val LISTEN_WINDOW_MS = 8_000L

        fun createStartIntent(context: Context): Intent {
            return Intent(context, BlowDetectionService::class.java).setAction(ACTION_ARM)
        }

        fun createStopIntent(context: Context): Intent {
            return Intent(context, BlowDetectionService::class.java).setAction(ACTION_DISARM)
        }

        fun createDetectIntent(
            context: Context,
            notificationKey: String
        ): Intent {
            return Intent(context, BlowDetectionService::class.java)
                .setAction(ACTION_DETECT)
                .putExtra(EXTRA_NOTIFICATION_KEY, notificationKey)
        }

        fun tryArm(context: Context): Boolean {
            return try {
                context.startForegroundService(createStartIntent(context))
                true
            } catch (exception: IllegalStateException) {
                Log.w(TAG, "Android rejected the foreground service start request.", exception)
                false
            } catch (exception: SecurityException) {
                Log.w(TAG, "Foreground microphone service start is not currently eligible.", exception)
                false
            }
        }

        fun ensureNotificationChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) != null) {
                return
            }

            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.foreground_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.foreground_channel_description)
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }

            manager.createNotificationChannel(channel)
        }

        fun isArmed(): Boolean = isArmed
    }
}
