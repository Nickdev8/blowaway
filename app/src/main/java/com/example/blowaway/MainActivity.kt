package com.example.blowaway

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
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
            requestAudioPermission()
        }

        findViewById<Button>(R.id.openNotificationAccessButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
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
        if (requestCode == REQUEST_RECORD_AUDIO) {
            val granted = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            Toast.makeText(
                this,
                if (granted) R.string.audio_permission_granted else R.string.audio_permission_denied,
                Toast.LENGTH_SHORT
            ).show()
            updateStatus()
        }
    }

    private fun requestAudioPermission() {
        if (hasAudioPermission()) {
            Toast.makeText(this, R.string.audio_permission_already_granted, Toast.LENGTH_SHORT).show()
            return
        }

        requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
    }

    private fun updateStatus() {
        val audioStatus = if (hasAudioPermission()) {
            getString(R.string.status_granted)
        } else {
            getString(R.string.status_missing)
        }

        val listenerStatus = if (hasNotificationAccess()) {
            getString(R.string.status_enabled)
        } else {
            getString(R.string.status_disabled)
        }

        statusText.text = getString(R.string.status_template, audioStatus, listenerStatus)
    }

    private fun hasAudioPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
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
        private const val REQUEST_RECORD_AUDIO = 1001
    }
}
