package com.androidaiagent.runtime

import android.content.Context
import android.graphics.Rect
import android.provider.Settings
import com.androidaiagent.core.eventbus.EventBus
import com.androidaiagent.core.eventbus.GlobalEventBus
import com.androidaiagent.core.pipeline.VisionPipeline
import com.androidaiagent.core.SharedAppState
import com.androidaiagent.core.ScreenStateStream
import com.androidaiagent.execution.ExecutionAuthority
import com.androidaiagent.execution.SafeExecutionZone
import com.androidaiagent.metrics.HealthMonitor
import com.androidaiagent.routing.RouteEngine
import com.androidaiagent.routing.RouteGraph
import com.androidaiagent.routing.RouteTransactionJournal
import com.androidaiagent.routing.RoutingDecisionPolicy
import com.androidaiagent.runtime.RuntimeScheduler
import com.androidaiagent.runtime.RuntimePhase
import com.androidaiagent.runtime.RuntimeMode
import com.androidaiagent.runtime.RuntimeStartupValidator
import com.androidaiagent.state.StateEngine
import com.androidaiagent.core.taskengine.TaskEngine
import com.androidaiagent.safety.SafetyValidator
import com.androidaiagent.ai.AIAssistant
import com.androidaiagent.action.ActionQueue
import com.androidaiagent.action.ActionExecutor
import com.androidaiagent.action.ExecutionResult
import com.androidaiagent.action.ExecutionVerifier
import com.androidaiagent.action.VerificationStatus
import com.androidaiagent.accessibility.AccessibilityService
import com.androidaiagent.screencapture.ScreenCaptureService
import com.androidaiagent.recovery.UnknownStateResolver
import com.androidaiagent.replay.ReplaySession
import com.androidaiagent.vision.stabilization.ScreenStabilizationLayer
import com.androidaiagent.vision.SemanticUiInterpreter
import com.androidaiagent.vision.ModalDetectionEngine
import com.androidaiagent.runtime.ResourceManager
import com.androidaiagent.learning.TrainingDataStore
import com.androidaiagent.diagnostics.DiagnosticsSystem
import com.androidaiagent.settings.AppSettingsStore
import com.androidaiagent.metrics.LatencyProfiler
import com.androidaiagent.metrics.LatencySnapshot
import com.androidaiagent.core.confidence.ConfidenceEngine
import com.androidaiagent.core.confidence.ConfidenceInputs
import com.androidaiagent.core.frame.FrameClock
import com.androidaiagent.core.frame.FrameSyncBarrier
import com.androidaiagent.coordinates.CoordinateSpaceManager
import com.androidaiagent.perception.PerceptionCache
import com.androidaiagent.perception.PersistentUiGraph
import com.androidaiagent.ui.model.UiMap
import com.androidaiagent.core.taskengine.DemoAutomationTasks
import com.androidaiagent.world.ExecutionStatus
import com.androidaiagent.world.WorldStateStore
import com.androidaiagent.action.ActionFingerprinting
import com.androidaiagent.diagnostics.ActionLogEntry
import com.androidaiagent.diagnostics.RuntimeTraceEntry
import com.androidaiagent.routing.RouteTransactionEntry
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AgentRuntimeManager(
    private val context: Context,
    private val eventBus: EventBus = GlobalEventBus,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val runtimeScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var mainLoopJob: Job? = null
    
    private val _runtimeState = MutableStateFlow(RuntimeState.STOPPED)
    val runtimeState: StateFlow<RuntimeState> = _runtimeState.asStateFlow()
    
    private val _healthStatus = MutableStateFlow(HealthStatus.HEALTHY)
    val healthStatus: StateFlow<HealthStatus> = _healthStatus.asStateFlow()
    
    private val _actionQueueState = MutableStateFlow<ActionQueue?>(null)
    val actionQueueStateFlow: StateFlow<ActionQueue?> = _actionQueueState.asStateFlow()
    
    private val _lastExecutionResult = MutableStateFlow<ExecutionResult?>(null)
    val lastExecutionResult: StateFlow<ExecutionResult?> = _lastExecutionResult.asStateFlow()
    
    private val _latencySnapshot = MutableStateFlow(LatencySnapshot())
    val latencySnapshot: StateFlow<LatencySnapshot> = _latencySnapshot.asStateFlow()

    val appState: SharedAppState get() = sharedAppState
    val screenStream: ScreenStateStream get() = screenStateStream
    val resourceState: ResourceManager get() = resourceManager
    val worldStateStore: WorldStateStore get() = worldStateStoreImpl
    val worldState: StateFlow<com.androidaiagent.world.WorldStateSnapshot>
        get() = worldStateStoreImpl.snapshot

    private val sharedAppState = SharedAppState()
    private val screenStateStream = ScreenStateStream()
    private val worldStateStoreImpl = WorldStateStore()
    private val stabilizationLayer = ScreenStabilizationLayer()
    private val semanticUiInterpreter = SemanticUiInterpreter()
    private val modalDetectionEngine = ModalDetectionEngine()
    private val executionVerifier = ExecutionVerifier()
    private val resourceManager = ResourceManager()
    private val trainingDataStore = TrainingDataStore(File(context.filesDir, "training"))
    private val diagnosticsSystem = DiagnosticsSystem()
    private val startupValidator = RuntimeStartupValidator(context)
    private val perceptionCache = PerceptionCache()
    private val persistentUiGraph = PersistentUiGraph()
    private val actionFingerprinting = ActionFingerprinting()
    private val routeTransactionJournal = RouteTransactionJournal()
    private val frameClock = FrameClock()
    private val frameSyncBarrier = FrameSyncBarrier()
    private val runtimeScheduler = RuntimeScheduler()
    private val routingDecisionPolicy = RoutingDecisionPolicy()
    private val confidenceEngine = ConfidenceEngine()
    private val latencyProfiler = LatencyProfiler()
    private val healthMonitor = HealthMonitor()
    private val unknownStateResolver = UnknownStateResolver()
    private val replaySession = ReplaySession()
    
    private var accessibilityService: AccessibilityService? = null
    private var screenCaptureService: ScreenCaptureService? = null
    private var visionPipeline: VisionPipeline? = null
    private var routeEngine: RouteEngine? = null
    private var stateEngine: StateEngine? = null
    private var taskEngine: TaskEngine? = null
    private var safetyValidator: SafetyValidator? = null
    private var aiAssistant: AIAssistant? = null
    private var actionQueue: ActionQueue? = null
    private var actionExecutor: ActionExecutor? = null
    private var executionAuthority: ExecutionAuthority? = null
    private var watchdogSupervisor: WatchdogSupervisor? = null
    private var aiProvider: com.androidaiagent.ai.AIProvider? = null
    private var runtimeMode: RuntimeMode = RuntimeMode.FULL_AUTONOMOUS
    private var panicMode = false
    private var consecutiveFailures = 0
    private var lastUiMap: UiMap? = null
    private var coordinateSpaceManager: CoordinateSpaceManager? = null
    private var currentFrameId: Long = 0L
    
    fun start() {
        if (_runtimeState.value == RuntimeState.RUNNING) return
        
        _runtimeState.value = RuntimeState.STARTING
        worldStateStoreImpl.updateRuntimeState(RuntimeState.STARTING)
        worldStateStoreImpl.updateRuntimeMode(runtimeMode)
        
        runtimeScope.launch {
            try {
                initializeServices()
                val startupValidation = startupValidator.validate(
                    accessibilityReady = accessibilityService != null,
                    overlayPermissionGranted = Settings.canDrawOverlays(context),
                    screenCaptureReady = screenCaptureService != null,
                    ocrReady = true,
                    aiProvider = aiProvider,
                    requireAiProvider = runtimeMode != RuntimeMode.FULL_AUTONOMOUS
                )
                if (!startupValidation.isValid) {
                    throw IllegalStateException("Startup validation failed: ${startupValidation.missingRequirements.joinToString(",")}")
                }
                startDeterministicFlow()
                _runtimeState.value = RuntimeState.RUNNING
                worldStateStoreImpl.updateRuntimeState(RuntimeState.RUNNING)
                sharedAppState.setRunning(true)
                eventBus.tryPublish(com.androidaiagent.core.eventbus.Event.StateChanged("runtime", "running"))
            } catch (e: Exception) {
                _runtimeState.value = RuntimeState.ERROR
                _healthStatus.value = HealthStatus.UNHEALTHY
                worldStateStoreImpl.updateRuntimeState(RuntimeState.ERROR)
                worldStateStoreImpl.updateHealth(HealthStatus.UNHEALTHY, e.message)
                sharedAppState.setRunning(false)
                eventBus.tryPublish(com.androidaiagent.core.eventbus.Event.ErrorOccurred(e.message ?: "Startup failed", "AgentRuntimeManager"))
            }
        }
    }

    fun runDemoTask() {
        panicMode = false
        consecutiveFailures = 0
        diagnosticsSystem.clearLogs()
        routeTransactionJournal.clear()
        actionFingerprinting.clear()
        diagnosticsSystem.initialize(File(context.filesDir, "diagnostics"))
        diagnosticsSystem.startRecording()
        replaySession.start("demo_rewards_collect")
        setRuntimeMode(RuntimeMode.FULL_AUTONOMOUS)
        runtimeScope.launch {
            while (taskEngine == null || actionQueue == null) {
                delay(50)
            }
            taskEngine?.addTask(DemoAutomationTasks.createRewardsTask(actionQueue!!))
        }

        if (_runtimeState.value == RuntimeState.STOPPED) {
            start()
        }
    }
    
    fun stop() {
        if (_runtimeState.value == RuntimeState.STOPPED) return
        
        _runtimeState.value = RuntimeState.STOPPING
        worldStateStoreImpl.updateRuntimeState(RuntimeState.STOPPING)
        mainLoopJob?.cancel()
        mainLoopJob = null
        
        runtimeScope.launch {
            try {
                shutdownServices()
                _runtimeState.value = RuntimeState.STOPPED
                worldStateStoreImpl.updateRuntimeState(RuntimeState.STOPPED)
                worldStateStoreImpl.clear()
                runtimeScheduler.reset()
                healthMonitor.reset()
                sharedAppState.setRunning(false)
                sharedAppState.reset()
                screenStateStream.clear()
                replaySession.stop()
                perceptionCache.clear()
                persistentUiGraph.clear()
                actionFingerprinting.clear()
                routeTransactionJournal.clear()
                eventBus.tryPublish(com.androidaiagent.core.eventbus.Event.StateChanged("runtime", "stopped"))
            } catch (e: Exception) {
                _runtimeState.value = RuntimeState.ERROR
                worldStateStoreImpl.updateRuntimeState(RuntimeState.ERROR)
                worldStateStoreImpl.updateHealth(HealthStatus.UNHEALTHY, e.message)
                eventBus.tryPublish(com.androidaiagent.core.eventbus.Event.ErrorOccurred(e.message ?: "Shutdown failed", "AgentRuntimeManager"))
            }
        }
    }
    
    fun pause() {
        if (_runtimeState.value != RuntimeState.RUNNING) return
        _runtimeState.value = RuntimeState.PAUSED
        worldStateStoreImpl.updateRuntimeState(RuntimeState.PAUSED)
        sharedAppState.setRunning(false)
        eventBus.tryPublish(com.androidaiagent.core.eventbus.Event.StateChanged("runtime", "paused"))
    }
    
    fun resume() {
        if (_runtimeState.value != RuntimeState.PAUSED) return
        _runtimeState.value = RuntimeState.RUNNING
        worldStateStoreImpl.updateRuntimeState(RuntimeState.RUNNING)
        sharedAppState.setRunning(true)
        eventBus.tryPublish(com.androidaiagent.core.eventbus.Event.StateChanged("runtime", "running"))
    }
    
    private suspend fun initializeServices() {
        accessibilityService = AccessibilityService()
        screenCaptureService = ScreenCaptureService()
        
        val ocrStage = com.androidaiagent.core.pipeline.OCRStage()
        val accessibilityStage = com.androidaiagent.core.pipeline.AccessibilityParseStage()
        val iconDetectionStage = com.androidaiagent.core.pipeline.IconDetectionStage()
        val templateMatchingStage = com.androidaiagent.core.pipeline.TemplateMatchingStage()
        val uiMapGenerationStage = com.androidaiagent.core.pipeline.UiMapGenerationStage()
        val routeMatchingStage = com.androidaiagent.core.pipeline.RouteMatchingStage()
        
        visionPipeline = VisionPipeline(
            ocrStage,
            accessibilityStage,
            iconDetectionStage,
            templateMatchingStage,
            uiMapGenerationStage,
            routeMatchingStage
        )
        
        routeEngine = RouteEngine()
        routeEngine?.registerRoutes(DemoAutomationTasks.createRoutes())
        stateEngine = StateEngine()
        taskEngine = TaskEngine(runtimeScope)
        safetyValidator = SafetyValidator()
        
        aiProvider = if (runtimeMode != RuntimeMode.FULL_AUTONOMOUS) {
            com.androidaiagent.ai.AIProviderFactory.createProvider(
                provider = "openai",
                apiKey = AppSettingsStore.getApiKey(context)
            )
        } else {
            null
        }
        aiAssistant = aiProvider?.let { provider ->
            val contextMemory = com.androidaiagent.state.contextmemory.ContextMemory()
            val aiContextBuilder = com.androidaiagent.ai.AiContextBuilder(routeEngine!!.getRouteGraph())
            AIAssistant(provider, contextMemory, aiContextBuilder)
        }
        
        actionQueue = ActionQueue()
        _actionQueueState.value = actionQueue
        actionExecutor = ActionExecutor(accessibilityService!!)
        executionAuthority = ExecutionAuthority(actionQueue!!, SafeExecutionZone())
        watchdogSupervisor = WatchdogSupervisor(healthMonitor, actionQueue!!)
        worldStateStoreImpl.updateQueue(actionQueue!!.queueSize.value)
        worldStateStoreImpl.updateHealth(HealthStatus.HEALTHY)
        worldStateStoreImpl.updateExecutionOwner("AgentRuntimeManager")
    }
    
    private fun startDeterministicFlow() {
        mainLoopJob = runtimeScope.launch {
            while (isActive && _runtimeState.value != RuntimeState.STOPPING) {
                if (_runtimeState.value == RuntimeState.RUNNING) {
                    try {
                        processDeterministicFlow()
                    } catch (e: Exception) {
                        _healthStatus.value = HealthStatus.UNHEALTHY
                        eventBus.tryPublish(com.androidaiagent.core.eventbus.Event.ErrorOccurred(e.message ?: "Flow error", "DeterministicFlow"))
                        delay(1000)
                    }
                }
                delay(resourceManager.analysisIntervalMs.value)
            }
        }
    }
    
    private suspend fun processDeterministicFlow() {
        if (panicMode) {
            return
        }
        val now = System.currentTimeMillis()
        healthMonitor.reportOverlay(now)
        if (!runtimeScheduler.shouldRunPhase(RuntimePhase.PERCEPTION, now)) {
            return
        }
        latencyProfiler.markFrameStart()
        if (resourceManager.shouldThrottleCapture(now)) {
            return
        }

        val bitmap = screenCaptureService?.captureFrame() ?: return
        val frame = frameClock.tick(now)
        currentFrameId = frame.id
        frameSyncBarrier.markProduced(frame.id)
        resourceManager.markAnalysis(now)
        healthMonitor.reportFrame(now)
        if (coordinateSpaceManager == null) {
            coordinateSpaceManager = CoordinateSpaceManager(
                deviceWidth = context.resources.displayMetrics.widthPixels,
                deviceHeight = context.resources.displayMetrics.heightPixels,
                screenshotWidth = bitmap.width,
                screenshotHeight = bitmap.height,
                densityDpi = context.resources.displayMetrics.densityDpi
            )
        }
        
        screenStateStream.publishScreenshot(bitmap, now, frame.id)
        if (!frameSyncBarrier.canConsume(frame.id)) {
            return
        }

        val stabilization = stabilizationLayer.registerFrame(bitmap)
        sharedAppState.setScreenFreezeDetected(!stabilization.isStable || stabilization.isLoading)
        worldStateStoreImpl.updateFrame(
            frameId = frame.id,
            timestamp = now,
            screenshot = bitmap,
            uiMap = lastUiMap,
            appState = stateEngine?.currentState?.value,
            screenStable = stabilization.isStable && !stabilization.isLoading
        )
        worldStateStoreImpl.updateRuntimeState(_runtimeState.value)
        if (!runtimeScheduler.shouldProcessFrame(frame.id, stabilization.isStable)) {
            return
        }
        runtimeScheduler.markPhase(RuntimePhase.PERCEPTION, now)
        worldStateStoreImpl.updatePhase(RuntimePhase.PERCEPTION.name)
        if (!stabilization.isStable || stabilization.isTransitioning || stabilization.isLoading) {
            sharedAppState.setAIStatus(com.androidaiagent.core.AIStatus.WAITING)
            worldStateStoreImpl.updateAiStatus(com.androidaiagent.core.AIStatus.WAITING)
            _healthStatus.value = HealthStatus.HEALTHY
            worldStateStoreImpl.updateHealth(HealthStatus.HEALTHY)
            return
        }
        
        latencyProfiler.markOcrStart()
        val screenshotHash = hashBitmap(bitmap)
        val cachedUiMap = if (runtimeMode != RuntimeMode.DEBUG) {
            perceptionCache.get(screenshotHash, now)
        } else {
            null
        }
        val uiMap = cachedUiMap ?: visionPipeline?.processScreenshot(bitmap) ?: return
        if (cachedUiMap == null) {
            perceptionCache.put(screenshotHash, uiMap, now)
        }
        healthMonitor.reportOcr(now)
        screenStateStream.publishUiMap(uiMap)
        lastUiMap = uiMap
        val semanticUi = semanticUiInterpreter.interpret(uiMap)
        val modalDetection = modalDetectionEngine.detect(uiMap)
        if (semanticUi.modalDetected) {
            sharedAppState.setSafetyStatus(com.androidaiagent.core.SafetyStatus.WARNING)
            worldStateStoreImpl.updateSafety(com.androidaiagent.core.SafetyStatus.WARNING)
        } else if (sharedAppState.safetyStatus.value != com.androidaiagent.core.SafetyStatus.UNSAFE) {
            sharedAppState.setSafetyStatus(com.androidaiagent.core.SafetyStatus.SAFE)
            worldStateStoreImpl.updateSafety(com.androidaiagent.core.SafetyStatus.SAFE)
        }
        if (modalDetection.blocking) {
            sharedAppState.setSafetyStatus(com.androidaiagent.core.SafetyStatus.UNSAFE)
            worldStateStoreImpl.updateModal(modalDetection.kind.name, blocking = true)
            worldStateStoreImpl.updateSafety(com.androidaiagent.core.SafetyStatus.UNSAFE)
        } else {
            worldStateStoreImpl.updateModal(modalDetection.kind.name, blocking = false)
        }
        
        latencyProfiler.markRouteStart()
        if (!runtimeScheduler.shouldRunPhase(RuntimePhase.ROUTING, now)) {
            return
        }
        val routeMatch = routeEngine?.matchRoute(uiMap) ?: return
        val previousRoute = worldStateStoreImpl.snapshot.value.currentRoute
        sharedAppState.setCurrentRoute(routeMatch.routeName, routeMatch.confidence)
        worldStateStoreImpl.updateRoute(routeMatch.routeName, routeMatch.confidence)
        runtimeScheduler.markPhase(RuntimePhase.ROUTING, now)
        worldStateStoreImpl.updatePhase(RuntimePhase.ROUTING.name)
        persistentUiGraph.update(uiMap, routeMatch.routeName)
        val routeTransactionIndex = routeTransactionJournal.begin(routeMatch.routeName, routeMatch.routeName)
        worldStateStoreImpl.updateTransition("${previousRoute ?: "unknown"}->${routeMatch.routeName}")
        diagnosticsSystem.logRuntimeTrace(frame.id, RuntimePhase.ROUTING.name, routeMatch.routeName, null, "matched", latencyProfiler.snapshot().frameProcessingMs)
        val routeDecision = routingDecisionPolicy.choose(routeMatch, aiAssistant?.confidence?.value ?: 0f)
        val allowAiSupport = routeDecision != com.androidaiagent.routing.RoutingDecision.DETERMINISTIC && runtimeMode != RuntimeMode.FULL_AUTONOMOUS
        if (routeDecision == com.androidaiagent.routing.RoutingDecision.DETERMINISTIC) {
            sharedAppState.setAIStatus(com.androidaiagent.core.AIStatus.WAITING)
            worldStateStoreImpl.updateAiStatus(com.androidaiagent.core.AIStatus.WAITING)
        }
        if (routeMatch.confidence < 0.5f) {
            sharedAppState.setAIStatus(com.androidaiagent.core.AIStatus.WAITING)
            worldStateStoreImpl.updateAiStatus(com.androidaiagent.core.AIStatus.WAITING)
            trainingDataStore.saveAmbiguousState(uiMap, "low_route_confidence")
        }
        
        val appState = stateEngine?.evaluateState(uiMap, routeMatch) ?: return
        worldStateStoreImpl.updateFrame(
            frameId = frame.id,
            timestamp = now,
            uiMap = uiMap,
            appState = appState,
            screenStable = stabilization.isStable && !stabilization.isLoading
        )
        frameSyncBarrier.markConsumed(frame.id)
        runtimeScheduler.markProcessedFrame(frame.id)
        replaySession.record(worldStateStoreImpl.snapshot.value)

        val activeTask = taskEngine?.currentTask?.value
        if (activeTask != null) {
            sharedAppState.setCurrentTask(activeTask.goal)
            worldStateStoreImpl.updateTask(activeTask.goal)
            if (activeTask.requiredRoutes.isNotEmpty() && routeMatch.routeName !in activeTask.requiredRoutes) {
                sharedAppState.setAIStatus(com.androidaiagent.core.AIStatus.WAITING)
                worldStateStoreImpl.updateAiStatus(com.androidaiagent.core.AIStatus.WAITING)
                return
            }
        } else {
            worldStateStoreImpl.updateTask(null)
        }
        
        if (runtimeMode == RuntimeMode.PASSIVE_OBSERVE || runtimeMode == RuntimeMode.REPLAY) {
            routeTransactionJournal.commit(routeTransactionIndex, success = true)
            return
        }

        if (appState == com.androidaiagent.state.AppState.UNKNOWN) {
            trainingDataStore.saveUnknownScreen(uiMap, "unknown_state")
            val resolved = unknownStateResolver.resolve(uiMap, bitmap)
            if (resolved.shouldPause) {
                sharedAppState.setAIStatus(com.androidaiagent.core.AIStatus.WAITING)
                worldStateStoreImpl.updateAiStatus(com.androidaiagent.core.AIStatus.WAITING)
            }
            runtimeScheduler.markPhase(RuntimePhase.RECOVERY, now)
            worldStateStoreImpl.updateHealth(HealthStatus.RECOVERING)
            routeTransactionJournal.commit(routeTransactionIndex, success = false, reason = "unknown_state")
            diagnosticsSystem.logRuntimeTrace(frame.id, RuntimePhase.RECOVERY.name, routeMatch.routeName, null, "unknown_state", latencyProfiler.snapshot().frameProcessingMs)
            delay(500)
            return
        }
        
        if (sharedAppState.safetyStatus.value == com.androidaiagent.core.SafetyStatus.UNSAFE) {
            runtimeScheduler.markPhase(RuntimePhase.RECOVERY, now)
            worldStateStoreImpl.updateHealth(HealthStatus.RECOVERING)
            routeTransactionJournal.commit(routeTransactionIndex, success = false, reason = "unsafe_state")
            diagnosticsSystem.logRuntimeTrace(frame.id, RuntimePhase.RECOVERY.name, routeMatch.routeName, null, "unsafe_state", latencyProfiler.snapshot().frameProcessingMs)
            delay(1000)
            return
        }

        val currentTask = sharedAppState.currentTask.value ?: activeTask?.goal
        if (currentTask != null && routeMatch.confidence > 0.7f && allowAiSupport && runtimeScheduler.shouldRunPhase(RuntimePhase.AI, now)) {
            sharedAppState.setAIStatus(com.androidaiagent.core.AIStatus.PROCESSING)
            worldStateStoreImpl.updateAiStatus(com.androidaiagent.core.AIStatus.PROCESSING)
            latencyProfiler.markAiStart()
            healthMonitor.reportAi(now)
            
            val aiResponse = aiAssistant?.assistDecision(
                uiMap,
                currentTask,
                "You are a helper layer. Provide decision support only."
            )
            
            sharedAppState.setAIStatus(com.androidaiagent.core.AIStatus.IDLE)
            worldStateStoreImpl.updateAiStatus(com.androidaiagent.core.AIStatus.IDLE)
            runtimeScheduler.markPhase(RuntimePhase.AI, now)
            
            if (aiResponse != null) {
                val validationResult = safetyValidator?.validateAction(
                    aiResponse.action,
                    aiResponse.target,
                    emptyMap(),
                    uiMap
                )
                
                if (validationResult?.isValid == true) {
                    val fingerprint = actionFingerprinting.fingerprint(
                        aiResponse.action,
                        aiResponse.target,
                        routeMatch.routeName,
                        uiMap,
                        aiResponse.parameters
                    )
                    if (actionFingerprinting.shouldDeduplicate(fingerprint)) {
                        routeTransactionJournal.commit(routeTransactionIndex, success = false, reason = "duplicate_action")
                        return
                    }
                    val submitted = executionAuthority?.submit(
                        aiResponse,
                        source = "ai",
                        route = routeMatch.routeName,
                        confidence = routeMatch.confidence,
                        uiMap = uiMap
                    ) == true
                    if (submitted) {
                        worldStateStoreImpl.updateQueue(actionQueue?.queueSize?.value ?: 0)
                    }
                    sharedAppState.setLastAction("${aiResponse.action} on ${aiResponse.target}")
                    worldStateStoreImpl.updateAction("${aiResponse.action} on ${aiResponse.target}")
                }
            }
        } else {
            worldStateStoreImpl.updateAiStatus(com.androidaiagent.core.AIStatus.WAITING)
        }
        
        if (!runtimeScheduler.shouldRunPhase(RuntimePhase.EXECUTION, now)) {
            _healthStatus.value = HealthStatus.HEALTHY
            worldStateStoreImpl.updateHealth(HealthStatus.HEALTHY)
            _latencySnapshot.value = latencyProfiler.snapshot()
            worldStateStoreImpl.updateLatency(_latencySnapshot.value)
            return
        }

        val pendingAction = actionQueue?.dequeue()
        if (pendingAction != null) {
            val fingerprint = actionFingerprinting.fingerprint(
                pendingAction.action,
                pendingAction.target,
                pendingAction.route,
                uiMap,
                pendingAction.parameters
            )
            if (actionFingerprinting.shouldDeduplicate(fingerprint)) {
                actionQueue?.markFailed(pendingAction.id, "duplicate_action")
                worldStateStoreImpl.updateAction("${pendingAction.action} on ${pendingAction.target}", ExecutionStatus.FAILED)
                routeTransactionJournal.commit(routeTransactionIndex, success = false, reason = "duplicate_action")
                return
            }
            worldStateStoreImpl.updateAction("${pendingAction.action} on ${pendingAction.target}", ExecutionStatus.VALIDATING)
            runtimeScheduler.markPhase(RuntimePhase.EXECUTION, now)
            worldStateStoreImpl.updatePhase(RuntimePhase.EXECUTION.name)
            actionQueue?.markExecuting(pendingAction.id)
            worldStateStoreImpl.updateQueue(actionQueue?.queueSize?.value ?: 0)
            worldStateStoreImpl.updateAction("${pendingAction.action} on ${pendingAction.target}", ExecutionStatus.EXECUTING)
            val beforeUiMap = lastUiMap
            try {
                latencyProfiler.markActionStart()
                executionAuthority?.executeExclusive {
                    actionExecutor?.execute(
                        pendingAction.action,
                        pendingAction.target,
                        normalizeActionParameters(pendingAction.parameters)
                    )
                }
                actionQueue?.markAwaitingResult(pendingAction.id)
                worldStateStoreImpl.updateAction("${pendingAction.action} on ${pendingAction.target}", ExecutionStatus.AWAITING_RESULT)
                delay(250)

                val refreshedBitmap = screenCaptureService?.captureFrame()
                val afterUiMap = refreshedBitmap?.let {
                    val afterFrame = frameClock.tick(System.currentTimeMillis())
                    frameSyncBarrier.markProduced(afterFrame.id)
                    screenStateStream.publishScreenshot(it, afterFrame.timestamp, afterFrame.id)
                    val processed = visionPipeline?.processScreenshot(it)
                    if (processed != null) {
                        screenStateStream.publishUiMap(processed)
                        lastUiMap = processed
                    }
                    processed
                } ?: screenStateStream.currentUiMap.value

                val afterRoute = afterUiMap?.let { routeEngine?.matchRoute(it) }
                val routeVerified = verifyRouteTransition(
                    beforeUiMap = beforeUiMap,
                    afterUiMap = afterUiMap,
                    afterRoute = afterRoute,
                    expectedRoute = pendingAction.route ?: routeMatch.routeName
                )
                val verification = executionVerifier.verify(
                    pendingAction,
                    beforeUiMap,
                    afterUiMap,
                    routeMatch.routeName,
                    afterRoute
                )
                val executionConfidence = confidenceEngine.fuse(
                    ConfidenceInputs(
                        ocrConfidence = afterUiMap?.currentScreen?.textElements?.maxOfOrNull { it.confidence } ?: 0f,
                        routeConfidence = afterRoute?.confidence ?: 0f,
                        aiConfidence = aiAssistant?.confidence?.value ?: 0f,
                        detectorConfidence = if (modalDetection.blocking) 0.3f else 1f,
                        executionConfidence = if (verification.status == VerificationStatus.CONFIRMED) 1f else 0.2f
                    )
                )
                _lastExecutionResult.value = ExecutionResult(
                    actionId = pendingAction.id,
                    sourceFrameId = currentFrameId,
                    resultFrameId = currentFrameId,
                    beforeRoute = routeMatch.routeName,
                    afterRoute = afterRoute?.routeName,
                    detector = if (modalDetection.blocking) modalDetection.kind.name else "route",
                    verification = verification,
                    confidence = executionConfidence
                )
                worldStateStoreImpl.updateConfidence("execution", executionConfidence)
                diagnosticsSystem.logRuntimeTrace(
                    frame.id,
                    RuntimePhase.EXECUTION.name,
                    afterRoute?.routeName ?: routeMatch.routeName,
                    pendingAction.action.name,
                    verification.status.name,
                    latencyProfiler.snapshot().frameProcessingMs
                )
                if (executionConfidence < 0.45f) {
                    sharedAppState.setSafetyStatus(com.androidaiagent.core.SafetyStatus.WARNING)
                    worldStateStoreImpl.updateSafety(com.androidaiagent.core.SafetyStatus.WARNING)
                }

                when {
                    !routeVerified -> {
                        actionQueue?.markFailed(pendingAction.id, "route_verification_failed")
                        worldStateStoreImpl.updateAction("${pendingAction.action} on ${pendingAction.target}", ExecutionStatus.FAILED)
                        routeTransactionJournal.commit(routeTransactionIndex, success = false, reason = "route_verification_failed")
                        consecutiveFailures++
                    }
                    verification.status == VerificationStatus.CONFIRMED -> {
                        actionQueue?.markConfirmed(pendingAction.id)
                        worldStateStoreImpl.updateAction("${pendingAction.action} on ${pendingAction.target}", ExecutionStatus.CONFIRMED)
                        routeTransactionJournal.commit(routeTransactionIndex, success = true)
                        consecutiveFailures = 0
                    }
                    verification.status == VerificationStatus.AWAITING_RESULT -> {
                        actionQueue?.markAwaitingResult(pendingAction.id)
                        worldStateStoreImpl.updateAction("${pendingAction.action} on ${pendingAction.target}", ExecutionStatus.AWAITING_RESULT)
                    }
                    verification.status == VerificationStatus.FAILED -> {
                        actionQueue?.markFailed(pendingAction.id, verification.reason)
                        trainingDataStore.saveFailedRoute(afterRoute?.routeName, afterUiMap ?: beforeUiMap ?: uiMap, verification.reason)
                        if (routeMatch.routeName.isNotBlank() && afterRoute?.routeName != null) {
                            routeEngine?.getRouteGraph()?.invalidateTransition(routeMatch.routeName, afterRoute.routeName)
                        }
                        worldStateStoreImpl.updateAction("${pendingAction.action} on ${pendingAction.target}", ExecutionStatus.FAILED)
                        routeTransactionJournal.commit(routeTransactionIndex, success = false, reason = verification.reason)
                        consecutiveFailures++
                    }
                    verification.status == VerificationStatus.RECOVERED -> {
                        actionQueue?.markRecovered(pendingAction.id)
                        worldStateStoreImpl.updateAction("${pendingAction.action} on ${pendingAction.target}", ExecutionStatus.RECOVERED)
                        routeTransactionJournal.commit(routeTransactionIndex, success = true, reason = "recovered")
                        consecutiveFailures = 0
                    }
                }
                if (consecutiveFailures >= 3 || panicMode) {
                    triggerRuntimePanic("repeated_failures")
                }
            } finally {
                actionQueue?.markProcessed()
                worldStateStoreImpl.updateQueue(actionQueue?.queueSize?.value ?: 0)
            }
        }
        
        _healthStatus.value = HealthStatus.HEALTHY
        worldStateStoreImpl.updateHealth(HealthStatus.HEALTHY)
        healthMonitor.reportOverlay(now)
        _latencySnapshot.value = latencyProfiler.snapshot()
        worldStateStoreImpl.updateLatency(_latencySnapshot.value)
        watchdogSupervisor?.let { watchdog ->
            val (watchdogAction, healthSnapshot) = watchdog.inspect(actionQueue?.queueSize?.value ?: 0, now)
            worldStateStoreImpl.updateHealth(healthSnapshot.status, healthSnapshot.lastError)
            if (watchdogAction != WatchdogAction.NONE) {
                watchdog.handle(watchdogAction)
                sharedAppState.setSafetyStatus(com.androidaiagent.core.SafetyStatus.RECOVERING)
                worldStateStoreImpl.updateSafety(com.androidaiagent.core.SafetyStatus.RECOVERING)
                worldStateStoreImpl.updateRuntimeState(_runtimeState.value)
                if (watchdogAction == WatchdogAction.RESET_SESSION) {
                    triggerRuntimePanic("watchdog_reset_session")
                }
            }
        }
    }
    
    private suspend fun shutdownServices() {
        screenCaptureService?.onDestroy()
        screenCaptureService = null
        accessibilityService = null
        visionPipeline = null
        routeEngine = null
        stateEngine = null
        taskEngine = null
        safetyValidator = null
        aiAssistant = null
        actionQueue = null
        _actionQueueState.value = null
        executionAuthority = null
        watchdogSupervisor = null
        replaySession.clear()
        perceptionCache.clear()
        persistentUiGraph.clear()
        actionFingerprinting.clear()
        routeTransactionJournal.clear()
    }

    fun destroy() {
        stop()
        runtimeScope.cancel()
    }

    fun setRuntimeMode(mode: RuntimeMode) {
        runtimeMode = mode
        worldStateStoreImpl.updateRuntimeMode(mode)
    }

    fun getDemoProofSnapshot(): DemoProofSnapshot {
        return DemoProofSnapshot(
            worldState = worldStateStoreImpl.snapshot.value,
            routeTransactions = routeTransactionJournal.recent(),
            runtimeTraces = diagnosticsSystem.getRuntimeTraceLog(),
            actionLog = diagnosticsSystem.getActionLog()
        )
    }

    private fun verifyRouteTransition(
        beforeUiMap: UiMap?,
        afterUiMap: UiMap?,
        afterRoute: com.androidaiagent.routing.RouteMatch?,
        expectedRoute: String
    ): Boolean {
        val beforeText = beforeUiMap?.currentScreen?.textElements?.joinToString(" ") { it.text }.orEmpty()
        val afterText = afterUiMap?.currentScreen?.textElements?.joinToString(" ") { it.text }.orEmpty()
        val uiGraphNode = afterUiMap?.let { persistentUiGraph.getNode(it.currentScreen) }
        val modalBlocked = afterUiMap?.detectedRoute == "unknown" || afterUiMap?.currentScreen?.buttons.isNullOrEmpty()
        val ocrMatches = afterText.contains(expectedRoute, ignoreCase = true) || beforeText != afterText
        val uiGraphMatches = uiGraphNode?.routeName == expectedRoute || uiGraphNode?.routeName == afterRoute?.routeName
        val accessibilityMatches = afterUiMap?.currentScreen?.elements?.isNotEmpty() == true
        val routeMatches = afterRoute?.routeName == expectedRoute || afterRoute?.routeName == routeEngine?.currentRoute?.value?.routeName

        return ocrMatches && uiGraphMatches && accessibilityMatches && routeMatches && !modalBlocked
    }

    private fun triggerRuntimePanic(reason: String) {
        if (panicMode) return
        panicMode = true
        actionQueue?.clear()
        worldStateStoreImpl.updateHealth(HealthStatus.UNHEALTHY, reason)
        worldStateStoreImpl.updateSafety(com.androidaiagent.core.SafetyStatus.UNSAFE)
        worldStateStoreImpl.updatePhase("PANIC")
        diagnosticsSystem.stopRecording()
        diagnosticsSystem.exportLogs()
        _runtimeState.value = RuntimeState.ERROR
        _healthStatus.value = HealthStatus.UNHEALTHY
        mainLoopJob?.cancel()
        eventBus.tryPublish(com.androidaiagent.core.eventbus.Event.ErrorOccurred(reason, "RuntimePanic"))
    }

    private fun normalizeActionParameters(parameters: Map<String, Any>): Map<String, Any> {
        val manager = coordinateSpaceManager ?: return parameters
        val x = parameters["x"] as? Float
        val y = parameters["y"] as? Float
        val x1 = parameters["x1"] as? Float
        val y1 = parameters["y1"] as? Float
        val x2 = parameters["x2"] as? Float
        val y2 = parameters["y2"] as? Float
        val bounds = parameters["bounds"] as? Rect

        return parameters.toMutableMap().apply {
            if (x != null && y != null) {
                val normalized = manager.normalizeScreenshotToDevice(x, y)
                put("x", normalized.x)
                put("y", normalized.y)
            }
            if (x1 != null && y1 != null) {
                val normalized = manager.normalizeScreenshotToDevice(x1, y1)
                put("x1", normalized.x)
                put("y1", normalized.y)
            }
            if (x2 != null && y2 != null) {
                val normalized = manager.normalizeScreenshotToDevice(x2, y2)
                put("x2", normalized.x)
                put("y2", normalized.y)
            }
            if (bounds != null) {
                put("bounds", manager.normalizeRect(bounds))
            }
        }
    }

    private fun hashBitmap(bitmap: android.graphics.Bitmap): String {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        var hash = 17L
        for (i in pixels.indices step 100) {
            hash = hash * 31 + pixels[i]
        }
        return hash.toString()
    }
}

data class DemoProofSnapshot(
    val worldState: com.androidaiagent.world.WorldStateSnapshot,
    val routeTransactions: List<RouteTransactionEntry>,
    val runtimeTraces: List<RuntimeTraceEntry>,
    val actionLog: List<ActionLogEntry>
)

enum class RuntimeState {
    STOPPED,
    STARTING,
    RUNNING,
    PAUSED,
    STOPPING,
    ERROR
}

enum class HealthStatus {
    HEALTHY,
    UNHEALTHY,
    RECOVERING
}
