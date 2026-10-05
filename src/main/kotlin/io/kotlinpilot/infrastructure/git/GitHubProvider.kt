package io.kotlinpilot.infrastructure.git

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotlinpilot.core.git.GitProvider
import io.kotlinpilot.core.git.PullRequestInfo
import io.kotlinpilot.infrastructure.terminal.SafeTerminalService
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

private val logger = KotlinLogging.logger("GitHubProvider")

class GitHubProvider(
    private val terminalService: SafeTerminalService,
    private val httpClient: HttpClient? = null,
    private val githubToken: String? = System.getenv("GITHUB_TOKEN") ?: System.getProperty("GITHUB_TOKEN"),
    private val repoOwner: String? = System.getenv("GITHUB_REPOSITORY_OWNER") ?: System.getProperty("GITHUB_REPOSITORY_OWNER"),
    private val repoName: String? = System.getenv("GITHUB_REPOSITORY_NAME") ?: System.getProperty("GITHUB_REPOSITORY_NAME")
) : GitProvider {

    override suspend fun createBranch(branchName: String): Result<String> {
        val sanitized = branchName.replace(Regex("[^a-zA-Z0-9/_-]"), "-")
        val result = terminalService.execute("git checkout -b $sanitized")
        return if (result.success) {
            Result.success("Switched to new branch: $sanitized")
        } else {
            Result.failure(Exception("Failed to create branch $sanitized: ${result.stderr}"))
        }
    }

    override suspend fun pushBranch(branchName: String, remote: String): Result<String> {
        val sanitized = branchName.replace(Regex("[^a-zA-Z0-9/_-]"), "-")
        val result = terminalService.execute("git push -u $remote $sanitized")
        return if (result.success) {
            Result.success(result.stdout)
        } else {
            Result.failure(Exception("Failed to push branch $sanitized to $remote: ${result.stderr}"))
        }
    }

    override suspend fun createPullRequest(pr: PullRequestInfo): Result<PullRequestInfo> {
        if (githubToken.isNullOrBlank() || repoOwner.isNullOrBlank() || repoName.isNullOrBlank() || httpClient == null) {
            logger.info { "GitHub credentials not fully configured; simulating PR creation." }
            return Result.success(
                pr.copy(
                    url = "https://github.com/${repoOwner ?: "owner"}/${repoName ?: "repo"}/pull/simulated-1",
                    number = 1
                )
            )
        }

        return try {
            val response: HttpResponse = httpClient.post("https://api.github.com/repos/$repoOwner/$repoName/pulls") {
                header(HttpHeaders.Authorization, "Bearer $githubToken")
                header(HttpHeaders.Accept, "application/vnd.github+json")
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("title", pr.title)
                        put("body", pr.body)
                        put("head", pr.headBranch)
                        put("base", pr.baseBranch)
                        put("draft", pr.draft)
                    }.toString()
                )
            }

            if (response.status.isSuccess()) {
                val bodyText = response.bodyAsText()
                val json = Json { ignoreUnknownKeys = true }
                val jsonElement = json.parseToJsonElement(bodyText).jsonObject
                val htmlUrl = jsonElement["html_url"]?.jsonPrimitive?.content
                val number = jsonElement["number"]?.jsonPrimitive?.intOrNull
                Result.success(pr.copy(url = htmlUrl, number = number))
            } else {
                Result.failure(Exception("GitHub PR creation failed with ${response.status}: ${response.bodyAsText()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
