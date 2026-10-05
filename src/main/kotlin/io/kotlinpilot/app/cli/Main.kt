package io.kotlinpilot.app.cli

import io.kotlinpilot.core.domain.TaskMode
import io.kotlinpilot.infrastructure.config.KotlinPilotConfig
import java.nio.file.Paths

fun main(args: Array<String>) {
    val config = KotlinPilotConfig.load()
    val cli = KotlinPilotCli(config)

    if (args.isEmpty()) {
        printHelp()
        return
    }

    when (val command = args[0].lowercase()) {
        "analyze" -> {
            cli.analyze()
        }
        "task" -> {
            if (args.size < 2) {
                println("Error: Missing task description. Usage: kotlinpilot task \"<description>\"")
                return
            }
            val taskDescription = args[1]
            var mode = config.defaultMode
            var provider: String? = null
            var model: String? = null

            var i = 2
            while (i < args.size) {
                when (args[i]) {
                    "--explain" -> mode = TaskMode.EXPLAIN
                    "--assisted" -> mode = TaskMode.ASSISTED
                    "--autonomous" -> mode = TaskMode.AUTONOMOUS
                    "--provider" -> if (i + 1 < args.size) provider = args[++i]
                    "--model" -> if (i + 1 < args.size) model = args[++i]
                }
                i++
            }

            cli.runTask(taskDescription, mode = mode, provider = provider, model = model)
        }
        "status" -> {
            cli.status()
        }
        "diff" -> {
            val path = if (args.size > 1) args[1] else null
            cli.diff(path)
        }
        "review" -> {
            cli.review()
        }
        "server" -> {
            var port = 8080
            var i = 1
            while (i < args.size) {
                if ((args[i] == "--port" || args[i] == "-p") && i + 1 < args.size) {
                    port = args[++i].toIntOrNull() ?: 8080
                }
                i++
            }
            cli.startServer(port)
        }
        "help", "--help", "-h" -> {
            printHelp()
        }
        else -> {
            println("Unknown command: '$command'")
            printHelp()
        }
    }
}

private fun printHelp() {
    println("""
        KotlinPilot - Autonomous Kotlin & Android Engineering Agent
        
        Usage:
          kotlinpilot <command> [options]
        
        Commands:
          analyze                        Inspect repository, detect Kotlin/Android/Compose/KMP, architecture and rules
          task "<instruction>"           Execute an autonomous/assisted engineering task
          server [--port 8080]           Start REST API & Web Dashboard server with live SSE streaming
          status                         Show current Git status and branch
          diff [path]                    Show current Git diff of working tree
          review                         Run automated code review and pattern checks on current changes
          help                           Show this help message
        
        Task Options:
          --autonomous                   Run in full autonomous mode (edits, builds, commits)
          --assisted                     Run in assisted mode (default)
          --explain                      Analyze and propose plan without modifying files
          --provider <name>              AI provider (e.g. 'groq' or 'fake')
          --model <model-name>           Target model (e.g. 'llama-3.3-70b-versatile')
    """.trimIndent())
}
