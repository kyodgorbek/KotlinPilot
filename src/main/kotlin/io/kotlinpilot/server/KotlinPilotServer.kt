package io.kotlinpilot.server

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotlinpilot.core.agent.CodeReviewer
import io.kotlinpilot.core.agent.KotlinPilotAgent
import io.kotlinpilot.core.domain.TaskMode
import io.kotlinpilot.infrastructure.analyzer.RepositoryAnalyzer
import io.kotlinpilot.infrastructure.config.KotlinPilotConfig
import io.kotlinpilot.infrastructure.git.GitService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.sse.*
import kotlinx.coroutines.flow.filter
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger("KotlinPilotServer")

@Serializable
data class SubmitTaskRequest(
    val description: String,
    val mode: TaskMode = TaskMode.ASSISTED,
    val provider: String? = null,
    val model: String? = null
)

class KotlinPilotServer(
    private val config: KotlinPilotConfig,
    private val taskManager: TaskManager,
    private val analyzer: RepositoryAnalyzer,
    private val gitService: GitService,
    private val codeReviewer: CodeReviewer,
    private val port: Int = 8080
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private var serverEngine: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null

    fun start(wait: Boolean = true) {
        logger.info { "Starting KotlinPilot REST API & Dashboard on http://localhost:$port" }

        val server = embeddedServer(CIO, port = port) {
            install(ContentNegotiation) {
                json(json)
            }
            install(CORS) {
                anyHost()
                allowHeader(HttpHeaders.ContentType)
                allowHeader(HttpHeaders.Authorization)
                allowMethod(HttpMethod.Options)
                allowMethod(HttpMethod.Put)
                allowMethod(HttpMethod.Patch)
                allowMethod(HttpMethod.Delete)
            }
            install(SSE)
            install(StatusPages) {
                exception<Throwable> { call, cause ->
                    logger.error(cause) { "Unhandled server error" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to (cause.message ?: "Unknown server error"))
                    )
                }
            }

            routing {
                // UI Dashboard
                get("/") {
                    call.respondText(DashboardHtml.render(), ContentType.Text.Html)
                }
                get("/dashboard") {
                    call.respondText(DashboardHtml.render(), ContentType.Text.Html)
                }

                // Health
                get("/health") {
                    call.respond(
                        mapOf(
                            "status" to "UP",
                            "version" to "0.1.0",
                            "provider" to config.aiProvider,
                            "workspace" to config.projectWorkspace.toString()
                        )
                    )
                }

                // Project Architecture Inspection
                get("/api/project") {
                    val analysis = analyzer.analyze()
                    call.respond(analysis)
                }

                // Git Status & Diff
                get("/api/status") {
                    val branch = gitService.getCurrentBranch()
                    val status = gitService.status()
                    call.respond(mapOf("branch" to branch, "status" to status))
                }

                get("/api/diff") {
                    val diff = gitService.diff()
                    call.respond(mapOf("diff" to diff))
                }

                // Code Review on working tree
                post("/api/review") {
                    val analysis = analyzer.analyze()
                    val review = codeReviewer.review(analysis)
                    call.respond(review)
                }

                // Tasks API
                post("/api/tasks") {
                    val req = call.receive<SubmitTaskRequest>()
                    val task = taskManager.submitTask(
                        description = req.description,
                        mode = req.mode,
                        providerName = req.provider,
                        modelName = req.model
                    )
                    call.respond(HttpStatusCode.Created, task)
                }

                get("/api/tasks") {
                    call.respond(taskManager.listTasks())
                }

                get("/api/tasks/{id}") {
                    val id = call.parameters["id"]
                    val record = id?.let { taskManager.getTaskRecord(it) }
                    if (record == null) {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Task not found"))
                    } else {
                        call.respond(record.task)
                    }
                }

                // Live SSE Event Stream
                sse("/api/tasks/{id}/events") {
                    val taskId = call.parameters["id"] ?: return@sse
                    val record = taskManager.getTaskRecord(taskId)
                    
                    // Send historical events first
                    record?.events?.forEach { event ->
                        send(ServerSentEvent(data = json.encodeToString(event)))
                    }

                    // Stream incoming events
                    taskManager.eventFlow
                        .filter { it.taskId.value == taskId }
                        .collect { event ->
                            send(ServerSentEvent(data = json.encodeToString(event)))
                        }
                }
            }
        }

        serverEngine = server
        server.start(wait = wait)
    }

    fun stop() {
        serverEngine?.stop(1000, 2000)
    }
}
