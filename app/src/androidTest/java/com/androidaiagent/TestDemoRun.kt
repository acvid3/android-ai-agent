package com.androidaiagent

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androidaiagent.core.taskengine.DemoAutomationTasks
import com.androidaiagent.core.taskengine.TaskEngine
import com.androidaiagent.core.taskengine.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TestDemoRun {
    @Test
    fun demoTaskContractIsDeterministicAndRunnable() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val routeNames = DemoAutomationTasks.createRoutes().map { it.name }

        assertEquals("com.androidaiagent", context.packageName)
        assertTrue(routeNames.containsAll(listOf("home", "rewards", "popup")))

        val taskEngine = TaskEngine(kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined))
        val task = DemoAutomationTasks.createRewardsTask(
            queue = com.androidaiagent.action.ActionQueue()
        )

        assertEquals("demo_rewards_collect", task.id)
        assertFalse(task.validationRules.isEmpty())
        assertEquals("FULL_AUTONOMOUS", task.parameters["mode"])
        assertTrue(task.completionContract != null)

        taskEngine.addTask(task)
        assertTrue(
            taskEngine.taskStatus.value == TaskStatus.RUNNING ||
                taskEngine.taskStatus.value == TaskStatus.COMPLETED ||
                taskEngine.taskStatus.value == TaskStatus.IDLE
        )
    }
}
