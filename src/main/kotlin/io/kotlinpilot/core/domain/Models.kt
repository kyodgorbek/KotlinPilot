package io.kotlinpilot.core.domain

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class TaskStatus {
    CREATED,
    ANALYZING,
    PLANNING,
    WAITING_FOR_APPROVAL,
    IMPLEMENTING,
    BUILDING,
    TESTING,
    DEBUGGING,
    REVIEWING,
    COMMITTING,
    CREATING_PR,
    COMPLETED,
    FAILED,
    CANCELLED
}

@Serializable
enum class TaskMode {
    EXPLAIN,
    ASSISTED,
    AUTONOMOUS
}

@Serializable
data class TaskId(val value: String = UUID.randomUUID().toString().take(8)) {
    override fun toString(): String = value
}

@Serializable
data class Task(
    val id: TaskId = TaskId(),
    val description: String,
    val workspacePath: String,
    val mode: TaskMode = TaskMode.ASSISTED,
    val status: TaskStatus = TaskStatus.CREATED,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val maxIterations: Int = 20,
    val currentIteration: Int = 0,
    val summary: String? = null,
    val error: String? = null
)

@Serializable
enum class EventType {
    TASK_CREATED,
    ANALYSIS_STARTED,
    ANALYSIS_COMPLETED,
    PLAN_CREATED,
    TOOL_STARTED,
    TOOL_COMPLETED,
    BUILD_STARTED,
    BUILD_COMPLETED,
    TEST_STARTED,
    TEST_COMPLETED,
    ERROR_DETECTED,
    FIX_STARTED,
    REVIEW_STARTED,
    REVIEW_COMPLETED,
    TASK_COMPLETED,
    TASK_FAILED
}

@Serializable
data class TaskEvent(
    val taskId: TaskId,
    val type: EventType,
    val message: String,
    val payload: Map<String, String> = emptyMap(),
    val timestampEpochMs: Long = System.currentTimeMillis()
)

@Serializable
enum class Severity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

@Serializable
data class CodeReviewFinding(
    val severity: Severity,
    val file: String,
    val line: Int? = null,
    val description: String,
    val recommendation: String
)

@Serializable
data class CodeReviewResult(
    val approved: Boolean,
    val summary: String,
    val findings: List<CodeReviewFinding> = emptyList()
)

@Serializable
data class ModuleInfo(
    val name: String,
    val path: String,
    val isAndroid: Boolean = false,
    val isCompose: Boolean = false,
    val isKmp: Boolean = false,
    val dependencies: List<String> = emptyList()
)

@Serializable
data class ArchitectureHint(
    val category: String, // e.g. "UI", "DI", "Networking", "Database", "Architecture"
    val pattern: String,  // e.g. "Jetpack Compose", "Koin", "Ktor", "Room", "MVI"
    val confidence: Double = 1.0,
    val details: String = ""
)

@Serializable
data class ProjectAnalysis(
    val projectName: String,
    val rootPath: String,
    val isKotlin: Boolean,
    val isGradle: Boolean,
    val kotlinVersion: String? = null,
    val gradleVersion: String? = null,
    val agpVersion: String? = null,
    val composeEnabled: Boolean = false,
    val kmpEnabled: Boolean = false,
    val modules: List<ModuleInfo> = emptyList(),
    val architectureHints: List<ArchitectureHint> = emptyList(),
    val testFrameworks: List<String> = emptyList(),
    val customRules: String? = null,
    val importantFiles: List<String> = emptyList()
)
