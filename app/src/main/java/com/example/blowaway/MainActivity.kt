package com.example.blowaway

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)

        findViewById<Button>(R.id.requestAudioButton).setOnClickListener {
            requestRequiredPermissions()
        }

        findViewById<Button>(R.id.openNotificationAccessButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        findViewById<Button>(R.id.toggleBackgroundButton).setOnClickListener {
            toggleBackgroundListening()
        }

        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_REQUIRED_PERMISSIONS) {
            val granted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            Toast.makeText(
                this,
                if (granted) R.string.permissions_granted else R.string.permissions_denied,
                Toast.LENGTH_SHORT
            ).show()
            updateStatus()
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = buildList {
            if (!hasAudioPermission()) {
                add(Manifest.permission.RECORD_AUDIO)
            }
            if (!hasNotificationPermission()) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissions.isEmpty()) {
            Toast.makeText(this, R.string.permissions_already_granted, Toast.LENGTH_SHORT).show()
            return
        }

        requestPermissions(permissions.toTypedArray(), REQUEST_REQUIRED_PERMISSIONS)
    }

    private fun toggleBackgroundListening() {
        if (!hasAudioPermission() || !hasNotificationPermission()) {
            Toast.makeText(this, R.string.permissions_needed_for_background_mode, Toast.LENGTH_SHORT)
                .show()
            return
        }

        if (BlowDetectionService.isArmed()) {
            startService(BlowDetectionService.createStopIntent(this))
        } else {
            startForegroundService(BlowDetectionService.createStartIntent(this))
        }

        statusText.postDelayed({ updateStatus() }, 300)
    }

    private fun updateStatus() {
        val audioStatus = if (hasAudioPermission()) {
            getString(R.string.status_granted)
        } else {
            getString(R.string.status_missing)
        }

        val notificationPermissionStatus = if (hasNotificationPermission()) {
            getString(R.string.status_granted)
        } else {
            getString(R.string.status_missing)
        }

        val listenerStatus = if (hasNotificationAccess()) {
            getString(R.string.status_enabled)
        } else {
            getString(R.string.status_disabled)
        }

        val backgroundStatus = if (BlowDetectionService.isArmed()) {
            getString(R.string.status_armed)
        } else {
            getString(R.string.status_disarmed)
        }

        statusText.text = getString(
            R.string.status_template,
            audioStatus,
            notificationPermissionStatus,
            listenerStatus,
            backgroundStatus
        )

        findViewById<Button>(R.id.toggleBackgroundButton).text = getString(
            if (BlowDetectionService.isArmed()) {
                R.string.stop_background_button
            } else {
                R.string.start_background_button
            }
        )
    }

    private fun hasAudioPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun hasNotificationAccess(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        val targetComponent = ComponentName(this, BlowNotificationListener::class.java)

        return enabledListeners
            .split(':')
            .mapNotNull(ComponentName::unflattenFromString)
            .any { it == targetComponent }
    }

    companion object {
        private const val REQUEST_REQUIRED_PERMISSIONS = 1001
    }
}
