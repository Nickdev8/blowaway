package com.example.blowaway

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

object BackgroundModeController {
    fun stopBackgroundListening(context: Context) {
        context.startService(BlowDetectionService.createStopIntent(context))
    }

    fun tryAutoArm(context: Context): Boolean {
        if (BlowDetectionService.isArmed()) {
            return true
        }

        if (!hasRequiredSetup(context)) {
            return false
        }

        return BlowDetectionService.tryArm(context)
    }

    fun hasRequiredSetup(context: Context): Boolean {
        return hasAudioPermission(context) &&
            hasNotificationPermission(context) &&
            hasNotificationAccess(context)
    }

    fun hasAudioPermission(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasNotificationAccess(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        val targetComponent = ComponentName(context, BlowNotificationListener::class.java)

        return enabledListeners
            .split(':')
            .mapNotNull(ComponentName::unflattenFromString)
            .any { it == targetComponent }
    }
}
