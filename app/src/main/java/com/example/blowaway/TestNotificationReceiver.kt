package com.example.blowaway

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

class TestNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "POST_NOTIFICATIONS is not granted; skipping test notification.")
            return
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        ensureChannel(manager)

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "BlowAway test"
        val text = intent.getStringExtra(EXTRA_TEXT) ?: "Blow into the microphone now."

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setLocalOnly(true)
            .build()

        manager.notify(TEST_NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (manager.getNotificationChannel(CHANNEL_ID) != null) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            "BlowAway Test Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "ADB-triggered heads-up notifications for BlowAway testing."
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 250, 250)
        }

        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "TestNotificationRx"
        private const val CHANNEL_ID = "test_heads_up"
        private const val TEST_NOTIFICATION_ID = 3001
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_TEXT = "text"
    }
}
