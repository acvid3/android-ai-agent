package com.androidaiagent

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        apiKeyInput = findViewById(R.id.apiKeyInput)
        trackingSwitch = findViewById(R.id.trackingSwitch)
        statusText = findViewById(R.id.statusText)

        val saveButton: Button = findViewById(R.id.saveButton)
        val startButton: Button = findViewById(R.id.startButton)
        val enableAccessibilityButton: Button = findViewById(R.id.enableAccessibilityButton)

        loadSettings()

        trackingSwitch.setOnCheckedChangeListener { _, _ ->
            saveSettings()
        }

        saveButton.setOnClickListener {
            saveSettings()
        }

        startButton.setOnClickListener {
            saveSettings()
            startOverlayService()
        }

        enableAccessibilityButton.setOnClickListener {
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
        val intent = Intent(this, OverlayService::class.java)
        ContextCompat.startForegroundService(this, intent)

        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }
}
