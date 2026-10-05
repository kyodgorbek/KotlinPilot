package io.kotlinpilot.server

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotlinpilot.core.agent.AgentExecutionResult
import io.kotlinpilot.core.agent.KotlinPilotAgent
import io.kotlinpilot.core.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger("TaskManager")

data class TaskRecord(
    val task: Task,
    val events: MutableList<TaskEvent> = mutableListOf(),
    var executionResult: AgentExecutionResult? = null
)

class TaskManager(
    private val agentFactory: (providerName: String?, modelName: String?) -> KotlinPilotAgent,
    private val workspacePath: String,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val tasks = ConcurrentHashMap<String, TaskRecord>()
    private val _eventFlow = MutableSharedFlow<TaskEvent>(replay = 50, extraBufferCapacity = 100)
    val eventFlow: SharedFlow<TaskEvent> = _eventFlow.asSharedFlow()

    fun submitTask(
        description: String,
        mode: TaskMode = TaskMode.ASSISTED,
        providerName: String? = null,
        modelName: String? = null
    ): Task {
        val task = Task(
            description = description,
            workspacePath = workspacePath,
            mode = mode,
            status = TaskStatus.CREATED
        )
        val record = TaskRecord(task)
        tasks[task.id.value] = record

        scope.launch {
            val agent = agentFactory(providerName, modelName)
            try {
                val result = agent.executeTask(task) { event ->
                    record.events.add(event)
                    _eventFlow.tryEmit(event)
                }
                record.executionResult = result
                tasks[task.id.value] = record.copy(task = result.task)
            } catch (e: Exception) {
                logger.error(e) { "Error executing background task ${task.id}" }
                val failedTask = task.copy(status = TaskStatus.FAILED, error = e.message)
                val failEvent = TaskEvent(
                    taskId = task.id,
                    type = EventType.TASK_FAILED,
                    message = "Task execution threw exception: ${e.message}"
                )
                record.events.add(failEvent)
                _eventFlow.tryEmit(failEvent)
                tasks[task.id.value] = record.copy(task = failedTask)
            }
        }

        return task
    }

    fun getTask(id: String): Task? = tasks[id]?.task

    fun getTaskRecord(id: String): TaskRecord? = tasks[id]

    fun listTasks(): List<Task> = tasks.values.map { it.task }.sortedByDescending { it.createdAtEpochMs }

    fun getEvents(id: String): List<TaskEvent> = tasks[id]?.events ?: emptyList()
}
