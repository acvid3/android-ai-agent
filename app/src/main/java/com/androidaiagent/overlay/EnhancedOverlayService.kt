package com.androidaiagent.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.androidaiagent.runtime.AgentRuntimeManager
import com.androidaiagent.runtime.RuntimeState
import com.androidaiagent.world.WorldStateSnapshot
import com.androidaiagent.world.WorldStateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EnhancedOverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: LinearLayout? = null
    private var settingsPanel: LinearLayout? = null
    private var isExpanded = false
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var worldStateStore: WorldStateStore? = null
    private var onEmergencyStop: (() -> Unit)? = null
    private var worldStateJob: Job? = null
    
    private lateinit var statusIndicator: ImageView
    private lateinit var routeText: TextView
    private lateinit var frameText: TextView
    private lateinit var taskText: TextView
    private lateinit var aiStatusText: TextView
    private lateinit var lastActionText: TextView
    private lateinit var confidenceText: TextView
    private lateinit var healthText: TextView
    private lateinit var modeText: TextView
    private lateinit var phaseText: TextView
    private lateinit var transitionText: TextView
    private lateinit var ownerText: TextView
    private lateinit var screenshotPreview: ImageView
    private lateinit var emergencyStopText: TextView
    private lateinit var actionStateText: TextView
    private lateinit var queueSizeText: TextView
    private lateinit var latencyText: TextView
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlay()
    }

    fun bindRuntimeManager(manager: AgentRuntimeManager) {
        worldStateStore = manager.worldStateStore
        onEmergencyStop = manager::stop
        observeWorldState()
    }
    
    private fun createOverlay() {
        overlayView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(0xCC000000.toInt())
        }
        
        statusIndicator = ImageView(this).apply {
            setImageResource(android.R.drawable.presence_online)
        }
        
        routeText = TextView(this).apply {
            text = "Route: Unknown"
            setTextColor(0xFFFFFFFF.toInt())
        }

        frameText = TextView(this).apply {
            text = "Frame: 0"
            setTextColor(0xFFFFFFFF.toInt())
        }
        
        taskText = TextView(this).apply {
            text = "Task: None"
            setTextColor(0xFFFFFFFF.toInt())
        }
        
        aiStatusText = TextView(this).apply {
            text = "AI: Idle"
            setTextColor(0xFFFFFFFF.toInt())
        }

        lastActionText = TextView(this).apply {
            text = "Action: None"
            setTextColor(0xFFFFFFFF.toInt())
        }
        
        confidenceText = TextView(this).apply {
            text = "Confidence: 0%"
            setTextColor(0xFFFFFFFF.toInt())
        }

        healthText = TextView(this).apply {
            text = "Health: Unknown"
            setTextColor(0xFFFFFFFF.toInt())
        }

        modeText = TextView(this).apply {
            text = "Mode: Assisted"
            setTextColor(0xFFFFFFFF.toInt())
        }

        phaseText = TextView(this).apply {
            text = "Phase: idle"
            setTextColor(0xFFFFFFFF.toInt())
        }

        transitionText = TextView(this).apply {
            text = "Transition: none"
            setTextColor(0xFFFFFFFF.toInt())
        }

        ownerText = TextView(this).apply {
            text = "Owner: none"
            setTextColor(0xFFFFFFFF.toInt())
        }

        emergencyStopText = TextView(this).apply {
            text = "Emergency Stop"
            setTextColor(0xFFFF6B6B.toInt())
            setPadding(0, 12, 0, 12)
            setOnClickListener { onEmergencyStop?.invoke() }
        }

        actionStateText = TextView(this).apply {
            text = "Action: None"
            setTextColor(0xFFFFFFFF.toInt())
        }

        queueSizeText = TextView(this).apply {
            text = "Queue: 0"
            setTextColor(0xFFFFFFFF.toInt())
        }

        latencyText = TextView(this).apply {
            text = "Latency: 0/0/0"
            setTextColor(0xFFFFFFFF.toInt())
        }

        screenshotPreview = ImageView(this).apply {
            adjustViewBounds = true
            minimumWidth = 220
            minimumHeight = 220
        }
        
        overlayView?.addView(statusIndicator)
        overlayView?.addView(routeText)
        overlayView?.addView(frameText)
        overlayView?.addView(taskText)
        overlayView?.addView(aiStatusText)
        overlayView?.addView(lastActionText)
        overlayView?.addView(confidenceText)
        overlayView?.addView(healthText)
        overlayView?.addView(modeText)
        overlayView?.addView(phaseText)
        overlayView?.addView(transitionText)
        overlayView?.addView(ownerText)
        overlayView?.addView(actionStateText)
        overlayView?.addView(queueSizeText)
        overlayView?.addView(latencyText)
        overlayView?.addView(emergencyStopText)
        overlayView?.addView(screenshotPreview)
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 100
            y = 100
        }
        
        windowManager?.addView(overlayView, params)
        
        overlayView?.setOnClickListener {
            toggleSettingsPanel()
        }
    }
    
    private fun observeWorldState() {
        val store = worldStateStore ?: return
        worldStateJob?.cancel()
        worldStateJob = scope.launch {
            store.snapshot.collectLatest { snapshot ->
                renderSnapshot(snapshot)
            }
        }
    }

    private fun renderSnapshot(snapshot: WorldStateSnapshot) {
        updateStatusIndicator(snapshot.runtimeState)
        routeText.text = "Route: ${snapshot.currentRoute ?: "Unknown"}"
        frameText.text = "Frame: ${snapshot.frameId}"
        taskText.text = "Task: ${snapshot.currentTask ?: "None"}"
        lastActionText.text = snapshot.currentAction?.let {
            "Action: $it"
        } ?: "Action: None"
        confidenceText.text = "Confidence: ${(snapshot.routeConfidence * 100).toInt()}%"
        aiStatusText.text = "AI: ${snapshot.aiStatus.name}"
        healthText.text = "Health: ${snapshot.healthStatus.name}"
        modeText.text = "Mode: ${snapshot.runtimeMode.name}"
        phaseText.text = "Phase: ${snapshot.currentPhase ?: "idle"}"
        transitionText.text = "Transition: ${snapshot.currentTransition ?: "none"}"
        ownerText.text = "Owner: ${snapshot.executionAuthorityOwner ?: "none"}"
        actionStateText.text = snapshot.currentActionState?.let {
            "Action State: $it"
        } ?: "Action State: Idle"
        queueSizeText.text = "Queue: ${snapshot.queueSize}"
        latencyText.text = "Latency: ${snapshot.latency.frameProcessingMs}/${snapshot.latency.ocrMs}/${snapshot.latency.aiMs}"
        if (snapshot.screenshot != null) {
            screenshotPreview.setImageBitmap(snapshot.screenshot)
        } else {
            screenshotPreview.setImageDrawable(null)
        }
    }
    
    private fun updateStatusIndicator(state: RuntimeState) {
        val color = when (state) {
            RuntimeState.RUNNING -> android.R.drawable.presence_online
            RuntimeState.PAUSED -> android.R.drawable.presence_busy
            RuntimeState.ERROR -> android.R.drawable.presence_offline
            else -> android.R.drawable.presence_away
        }
        statusIndicator.setImageResource(color)
    }
    
    private fun toggleSettingsPanel() {
        isExpanded = !isExpanded
        if (isExpanded) {
            showSettingsPanel()
        } else {
            hideSettingsPanel()
        }
    }
    
    private fun showSettingsPanel() {
        
    }
    
    private fun hideSettingsPanel() {
        
    }
    
    fun updateRoute(route: String, confidence: Float) {
        routeText.text = "Route: $route"
        confidenceText.text = "Confidence: ${(confidence * 100).toInt()}%"
    }
    
    fun updateTask(task: String) {
        taskText.text = "Task: $task"
    }
    
    fun updateAIStatus(status: String) {
        aiStatusText.text = "AI: $status"
    }
    
    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let { windowManager?.removeView(it) }
        settingsPanel?.let { windowManager?.removeView(it) }
        worldStateJob?.cancel()
        scope.cancel()
        worldStateStore = null
        onEmergencyStop = null
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
