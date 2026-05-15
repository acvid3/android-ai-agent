package com.androidaiagent.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.androidaiagent.MainActivity
import com.androidaiagent.R
import com.androidaiagent.tracking.PatternSuggestion
import com.androidaiagent.tracking.PatternSuggestionStore
import com.androidaiagent.tracking.UserActionTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OverlayService : Service() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var suggestionJob: Job? = null
    private var windowManager: WindowManager? = null
    private var overlayButton: Button? = null
    private var suggestionView: LinearLayout? = null

    override fun onCreate() {
        super.onCreate()
        UserActionTracker.init(applicationContext)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()
        createOverlay()
        observeSuggestions()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (overlayButton == null && android.provider.Settings.canDrawOverlays(this)) {
            createOverlay()
        }
        return START_STICKY
    }

    private fun startAsForeground() {
        val channelId = "overlay_service_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "AI Agent Overlay",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("AI Agent running")
            .setContentText("Tracking and overlay are active")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)
    }

    private fun createOverlay() {
        if (!android.provider.Settings.canDrawOverlays(this)) return

        val button = Button(this).apply {
            text = "AI"
            setOnClickListener {
                val intent = Intent(this@OverlayService, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }
        overlayButton = button

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 32
            y = 120
        }

        windowManager?.addView(button, params)
    }

    private fun observeSuggestions() {
        suggestionJob?.cancel()
        suggestionJob = scope.launch {
            PatternSuggestionStore.currentSuggestion.collectLatest { suggestion ->
                if (suggestion == null) {
                    dismissSuggestion()
                } else {
                    showSuggestion(suggestion)
                }
            }
        }
    }

    private fun showSuggestion(suggestion: PatternSuggestion) {
        if (!android.provider.Settings.canDrawOverlays(this)) return
        dismissSuggestion()

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 20)
            setBackgroundColor(0xDD111111.toInt())

            addView(TextView(this@OverlayService).apply {
                text = getString(R.string.suggestion_title)
                setTextColor(0xFFFFFFFF.toInt())
            })

            addView(TextView(this@OverlayService).apply {
                text = "${suggestion.appPackage} • ${suggestion.actionType} x${suggestion.repeatCount}"
                setTextColor(0xFFB0B0B0.toInt())
            })

            addView(LinearLayout(this@OverlayService).apply {
                orientation = LinearLayout.HORIZONTAL

                addView(Button(this@OverlayService).apply {
                    text = getString(R.string.suggestion_yes)
                    setOnClickListener {
                        UserActionTracker.record(
                            com.androidaiagent.tracking.UserActionRecord(
                                appPackage = suggestion.appPackage,
                                timestamp = System.currentTimeMillis(),
                                actionType = "SUGGESTION_ACCEPTED",
                                uiContext = suggestion.uiContext,
                                text = suggestion.actionType
                            )
                        )
                        PatternSuggestionStore.dismiss()
                    }
                })

                addView(Button(this@OverlayService).apply {
                    text = getString(R.string.suggestion_no)
                    setOnClickListener {
                        PatternSuggestionStore.dismiss()
                    }
                })
            })
        }
        suggestionView = panel

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            x = 0
            y = 160
        }

        windowManager?.addView(panel, params)
    }

    private fun dismissSuggestion() {
        suggestionView?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        suggestionView = null
    }

    override fun onDestroy() {
        super.onDestroy()
        suggestionJob?.cancel()
        overlayButton?.let { runCatching { windowManager?.removeView(it) } }
        dismissSuggestion()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
