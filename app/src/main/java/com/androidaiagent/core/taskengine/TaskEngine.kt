package com.androidaiagent.core.taskengine

import com.androidaiagent.ui.model.UiMap
import com.androidaiagent.ai.ActionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.PriorityQueue

class TaskEngine(
    private val scope: CoroutineScope
) {
    private val taskQueue = PriorityQueue<TaskDefinition>(compareByDescending { it.priority.weight })
    private val activeTasks = mutableMapOf<String, TaskExecution>()
    private val cooldownManager = TaskCooldownManager()
    
    private val _currentTask = MutableStateFlow<TaskDefinition?>(null)
    val currentTask: StateFlow<TaskDefinition?> = _currentTask.asStateFlow()
    
    private val _taskStatus = MutableStateFlow<TaskStatus>(TaskStatus.IDLE)
    val taskStatus: StateFlow<TaskStatus> = _taskStatus.asStateFlow()
    
    private var executionJob: Job? = null
    
    fun addTask(task: TaskDefinition) {
        if (cooldownManager.isOnCooldown(task.id)) {
            return
        }
        
        taskQueue.add(task)
        startExecution()
    }
    
    fun addTasks(tasks: List<TaskDefinition>) {
        tasks.forEach { addTask(it) }
    }
    
    private fun startExecution() {
        if (executionJob?.isActive == true) return
        
        executionJob = scope.launch {
            while (taskQueue.isNotEmpty()) {
                val task = taskQueue.poll() ?: break
                
                if (cooldownManager.isOnCooldown(task.id)) {
                    continue
                }
                
                executeTask(task)
            }
            
            _taskStatus.value = TaskStatus.IDLE
            _currentTask.value = null
        }
    }
    
    private suspend fun executeTask(task: TaskDefinition) {
        _currentTask.value = task
        _taskStatus.value = TaskStatus.RUNNING
        
        val execution = TaskExecution(
            taskId = task.id,
            startTime = System.currentTimeMillis()
        )
        activeTasks[task.id] = execution
        
        try {
            task.execute(this, task.parameters)
            
            execution.success = true
            execution.endTime = System.currentTimeMillis()
            
            cooldownManager.setCooldown(task.id, task.cooldownDuration)
            
            _taskStatus.value = TaskStatus.COMPLETED
        } catch (e: Exception) {
            execution.success = false
            execution.error = e.message
            execution.endTime = System.currentTimeMillis()
            
            _taskStatus.value = TaskStatus.FAILED
        } finally {
            activeTasks.remove(task.id)
        }
    }
    
    fun pause() {
        executionJob?.cancel()
        _taskStatus.value = TaskStatus.PAUSED
    }
    
    fun resume() {
        if (_taskStatus.value == TaskStatus.PAUSED) {
            startExecution()
        }
    }
    
    fun cancelTask(taskId: String) {
        activeTasks.remove(taskId)
        taskQueue.removeIf { it.id == taskId }
    }
    
    fun cancelAll() {
        executionJob?.cancel()
        taskQueue.clear()
        activeTasks.clear()
        _taskStatus.value = TaskStatus.IDLE
        _currentTask.value = null
    }
    
    fun getTaskHistory(): List<TaskExecution> {
        return activeTasks.values.toList()
    }
}

data class TaskDefinition(
    val id: String,
    val description: String,
    val priority: TaskPriority,
    val goal: String = description,
    val requiredRoutes: List<String> = emptyList(),
    val requiredUiElements: List<String> = emptyList(),
    val expectedTransitions: List<String> = emptyList(),
    val validationRules: List<TaskValidationRule> = emptyList(),
    val completionContract: TaskCompletionContract? = null,
    val parameters: Map<String, Any> = emptyMap(),
    val cooldownDuration: Long = 0L,
    val execute: suspend (TaskEngine, Map<String, Any>) -> Unit
)

data class TaskExecution(
    val taskId: String,
    val startTime: Long,
    var endTime: Long = 0L,
    var success: Boolean = false,
    var error: String? = null
)

enum class TaskPriority(val value: Int) {
    CRITICAL(100),
    HIGH(75),
    MEDIUM(50),
    LOW(25);

    val weight: Int
        get() = value
}

enum class TaskStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED
}

class TaskCooldownManager {
    private val cooldowns = mutableMapOf<String, Long>()
    
    fun setCooldown(taskId: String, duration: Long) {
        if (duration > 0) {
            cooldowns[taskId] = System.currentTimeMillis() + duration
        }
    }
    
    fun isOnCooldown(taskId: String): Boolean {
        val expiry = cooldowns[taskId] ?: return false
        if (System.currentTimeMillis() >= expiry) {
            cooldowns.remove(taskId)
            return false
        }
        return true
    }
    
    fun getRemainingCooldown(taskId: String): Long {
        val expiry = cooldowns[taskId] ?: return 0
        val remaining = expiry - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0
    }
    
    fun clearCooldown(taskId: String) {
        cooldowns.remove(taskId)
    }
    
    fun clearAll() {
        cooldowns.clear()
    }
}

class TaskPlanner {
    fun plan(
        goal: String,
        currentRoute: String?,
        availableRoutes: List<String>,
        requiredElements: List<String> = emptyList()
    ): PlannedTask {
        val routeHints = mutableListOf<String>()
        currentRoute?.let { routeHints.add(it) }
        routeHints.addAll(availableRoutes.take(5))

        return PlannedTask(
            goal = goal,
            requiredRoutes = routeHints.distinct(),
            requiredUiElements = requiredElements,
            expectedTransitions = if (currentRoute == null) availableRoutes else listOfNotNull(currentRoute, availableRoutes.firstOrNull()),
            validationRules = listOf(
                TaskValidationRule("route_available", routeHints.isNotEmpty()),
                TaskValidationRule("ui_available", requiredElements.isNotEmpty() || availableRoutes.isNotEmpty())
            )
        )
    }
}

data class PlannedTask(
    val goal: String,
    val requiredRoutes: List<String>,
    val requiredUiElements: List<String>,
    val expectedTransitions: List<String>,
    val validationRules: List<TaskValidationRule>
)

data class TaskValidationRule(
    val name: String,
    val enabled: Boolean,
    val description: String = ""
)

data class TaskCompletionContract(
    val successConditions: List<String>,
    val failureConditions: List<String>,
    val retryBudget: Int,
    val rollbackStrategy: String
)
