package io.kotlinpilot.core.git

import kotlinx.serialization.Serializable

@Serializable
data class PullRequestInfo(
    val title: String,
    val body: String,
    val headBranch: String,
    val baseBranch: String = "main",
    val url: String? = null,
    val number: Int? = null,
    val draft: Boolean = false
)

interface GitProvider {
    suspend fun createBranch(branchName: String): Result<String>
    suspend fun pushBranch(branchName: String, remote: String = "origin"): Result<String>
    suspend fun createPullRequest(pr: PullRequestInfo): Result<PullRequestInfo>
}
