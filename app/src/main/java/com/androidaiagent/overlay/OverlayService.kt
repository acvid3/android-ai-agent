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
import android.util.Log
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

    companion object {
        private const val TAG = "OverlayService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: OverlayService starting")
        UserActionTracker.init(applicationContext)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()
        createOverlay()
        observeSuggestions()
        Log.d(TAG, "onCreate: OverlayService initialized successfully")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: Service command received")
        if (overlayButton == null && android.provider.Settings.canDrawOverlays(this)) {
            Log.d(TAG, "onStartCommand: Creating overlay (permission granted)")
            createOverlay()
        } else if (overlayButton == null) {
            Log.w(TAG, "onStartCommand: Cannot create overlay - permission not granted")
        } else {
            Log.d(TAG, "onStartCommand: Overlay already exists")
        }
        return START_STICKY
    }

    private fun startAsForeground() {
        Log.d(TAG, "startAsForeground: Starting foreground service with notification")
        val channelId = "overlay_service_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "AI Agent Overlay",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "startAsForeground: Notification channel created")
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
        Log.d(TAG, "startAsForeground: Foreground service started with notification ID 1001")
    }

    private fun createOverlay() {
        Log.d(TAG, "createOverlay: Attempting to create overlay")
        if (!android.provider.Settings.canDrawOverlays(this)) {
            Log.w(TAG, "createOverlay: Cannot create overlay - permission not granted")
            return
        }

        val button = Button(this).apply {
            text = "AI"
            setOnClickListener {
                Log.d(TAG, "Overlay button clicked - opening MainActivity")
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

        try {
            windowManager?.addView(button, params)
            Log.d(TAG, "createOverlay: Overlay button added successfully")
        } catch (e: Exception) {
            Log.e(TAG, "createOverlay: Failed to add overlay button", e)
        }
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
        Log.d(TAG, "showSuggestion: Showing suggestion for ${suggestion.appPackage} - ${suggestion.actionType} x${suggestion.repeatCount}")
        if (!android.provider.Settings.canDrawOverlays(this)) {
            Log.w(TAG, "showSuggestion: Cannot show suggestion - permission not granted")
            return
        }
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
                        Log.d(TAG, "Suggestion accepted by user")
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
                        Log.d(TAG, "Suggestion rejected by user")
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

        try {
            windowManager?.addView(panel, params)
            Log.d(TAG, "showSuggestion: Suggestion panel added successfully")
        } catch (e: Exception) {
            Log.e(TAG, "showSuggestion: Failed to add suggestion panel", e)
        }
    }

    private fun dismissSuggestion() {
        Log.d(TAG, "dismissSuggestion: Dismissing suggestion")
        suggestionView?.let { view ->
            runCatching {
                windowManager?.removeView(view)
                Log.d(TAG, "dismissSuggestion: Suggestion view removed")
            }
        }
        suggestionView = null
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: OverlayService destroying")
        super.onDestroy()
        suggestionJob?.cancel()
        overlayButton?.let {
            runCatching {
                windowManager?.removeView(it)
                Log.d(TAG, "onDestroy: Overlay button removed")
            }
        }
        dismissSuggestion()
        Log.d(TAG, "onDestroy: OverlayService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
