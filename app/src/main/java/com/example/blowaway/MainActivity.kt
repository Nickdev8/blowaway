package com.example.blowaway

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
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

        findViewById<Button>(R.id.openBlowDetectionChannelButton).setOnClickListener {
            openBlowDetectionChannelSettings()
        }

        findViewById<Button>(R.id.toggleBackgroundButton).setOnClickListener {
            toggleBackgroundListening()
        }

        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        syncBackgroundMode()
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
            syncBackgroundMode()
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = buildList {
            if (!BackgroundModeController.hasAudioPermission(this@MainActivity)) {
                add(Manifest.permission.RECORD_AUDIO)
            }
            if (!BackgroundModeController.hasNotificationPermission(this@MainActivity)) {
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
        if (!BackgroundModeController.hasAudioPermission(this) ||
            !BackgroundModeController.hasNotificationPermission(this)
        ) {
            Toast.makeText(this, R.string.permissions_needed_for_background_mode, Toast.LENGTH_SHORT)
                .show()
            return
        }

        if (BlowDetectionService.isArmed()) {
            BackgroundModeController.stopBackgroundListening(this)
        } else {
            BackgroundModeController.tryAutoArm(this)
        }

        refreshStatus()
    }

    private fun openBlowDetectionChannelSettings() {
        BlowDetectionService.ensureNotificationChannel(this)

        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, BlowDetectionService.CHANNEL_ID)
        }

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
            return
        }

        val fallbackIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }

        if (fallbackIntent.resolveActivity(packageManager) != null) {
            startActivity(fallbackIntent)
            return
        }

        Toast.makeText(this, R.string.notification_settings_unavailable, Toast.LENGTH_SHORT).show()
    }

    private fun syncBackgroundMode() {
        BackgroundModeController.tryAutoArm(this)
        refreshStatus()
    }

    private fun refreshStatus() {
        updateStatus()
        statusText.postDelayed({ updateStatus() }, 300)
    }

    private fun updateStatus() {
        val hasAudioPermission = BackgroundModeController.hasAudioPermission(this)
        val hasNotificationPermission = BackgroundModeController.hasNotificationPermission(this)
        val hasNotificationAccess = BackgroundModeController.hasNotificationAccess(this)
        val isBackgroundArmed = BlowDetectionService.isArmed()

        statusText.text = buildStatusText(
            audioReady = hasAudioPermission,
            notificationsReady = hasNotificationPermission,
            listenerReady = hasNotificationAccess,
            backgroundReady = isBackgroundArmed
        )

        findViewById<Button>(R.id.toggleBackgroundButton).text = getString(
            if (isBackgroundArmed) {
                R.string.stop_background_button
            } else {
                R.string.start_background_button
            }
        )
    }

    private fun buildStatusText(
        audioReady: Boolean,
        notificationsReady: Boolean,
        listenerReady: Boolean,
        backgroundReady: Boolean
    ): CharSequence {
        val builder = SpannableStringBuilder()

        appendStatusLine(
            builder,
            getString(R.string.status_label_microphone),
            if (audioReady) getString(R.string.status_granted) else getString(R.string.status_missing),
            audioReady
        )
        appendStatusLine(
            builder,
            getString(R.string.status_label_post_notifications),
            if (notificationsReady) getString(R.string.status_granted) else getString(R.string.status_missing),
            notificationsReady
        )
        appendStatusLine(
            builder,
            getString(R.string.status_label_notification_access),
            if (listenerReady) getString(R.string.status_enabled) else getString(R.string.status_disabled),
            listenerReady
        )
        appendStatusLine(
            builder,
            getString(R.string.status_label_background_mode),
            if (backgroundReady) getString(R.string.status_armed) else getString(R.string.status_disarmed),
            backgroundReady
        )

        return builder
    }

    private fun appendStatusLine(
        builder: SpannableStringBuilder,
        label: String,
        value: String,
        isReady: Boolean
    ) {
        if (builder.isNotEmpty()) {
            builder.append('\n')
        }

        val lineStart = builder.length
        val lineText = "$label: $value"
        builder.append(lineText)
        builder.setSpan(
            StyleSpan(Typeface.BOLD),
            lineStart,
            lineStart + label.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        if (!isReady) {
            builder.setSpan(
                ForegroundColorSpan(getColor(android.R.color.holo_red_dark)),
                lineStart,
                lineStart + lineText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
    companion object {
        private const val REQUEST_REQUIRED_PERMISSIONS = 1001
    }
}
