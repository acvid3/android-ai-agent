package com.androidaiagent

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.androidaiagent.overlay.OverlayService
import com.androidaiagent.settings.AppSettingsStore

class MainActivity : AppCompatActivity() {
    private lateinit var apiKeyInput: EditText
    private lateinit var trackingSwitch: Switch
    private lateinit var statusText: TextView

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: MainActivity started")
        setContentView(R.layout.activity_main)

        apiKeyInput = findViewById(R.id.apiKeyInput)
        trackingSwitch = findViewById(R.id.trackingSwitch)
        statusText = findViewById(R.id.statusText)

        val saveButton: Button = findViewById(R.id.saveButton)
        val startButton: Button = findViewById(R.id.startButton)
        val enableAccessibilityButton: Button = findViewById(R.id.enableAccessibilityButton)

        loadSettings()

        trackingSwitch.setOnCheckedChangeListener { _, _ ->
            Log.d(TAG, "Tracking switch changed: ${trackingSwitch.isChecked}")
            saveSettings()
        }

        saveButton.setOnClickListener {
            Log.d(TAG, "Save button clicked")
            saveSettings()
        }

        startButton.setOnClickListener {
            Log.d(TAG, "Start button clicked - initiating service start")
            saveSettings()
            startOverlayService()
        }

        enableAccessibilityButton.setOnClickListener {
            Log.d(TAG, "Opening accessibility settings")
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        refreshStatus()
    }

    private fun loadSettings() {
        apiKeyInput.setText(AppSettingsStore.getApiKey(this))
        trackingSwitch.isChecked = AppSettingsStore.isTrackingEnabled(this)
    }

    private fun saveSettings() {
        AppSettingsStore.save(
            context = this,
            apiKey = apiKeyInput.text?.toString().orEmpty(),
            trackingEnabled = trackingSwitch.isChecked
        )
        refreshStatus()
    }

    private fun refreshStatus() {
        val trackingStatus = if (trackingSwitch.isChecked) {
            getString(R.string.status_tracking_on)
        } else {
            getString(R.string.status_tracking_off)
        }
        statusText.text = buildString {
            append(getString(R.string.status_idle))
            append(" • ")
            append(trackingStatus)
        }
    }

    private fun startOverlayService() {
        Log.d(TAG, "startOverlayService: Starting overlay service")
        val intent = Intent(this, OverlayService::class.java)
        try {
            ContextCompat.startForegroundService(this, intent)
            Log.d(TAG, "startOverlayService: Service start command sent successfully")
        } catch (e: Exception) {
            Log.e(TAG, "startOverlayService: Failed to start service", e)
        }

        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "startOverlayService: Overlay permission not granted, requesting permission")
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        } else {
            Log.d(TAG, "startOverlayService: Overlay permission already granted")
        }
    }
}
