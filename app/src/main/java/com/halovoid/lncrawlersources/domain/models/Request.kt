package com.halovoid.lncrawler.domain.models

enum class RequestType {
    FULL_NOVEL,
    NOVEL_METADATA,
    VOLUME,
    CHAPTER,
    ARTIFACT
}

enum class RequestStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELLED,
    PAUSED
}
data class Request(
    val id: String,
    val name: String,
    val parentNovel: String?,
    val dependsOn: String? = null,
    val url: String?,
    val novelUrl: String,
    val priority: Int = 0,
    val type: RequestType,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long?,
    val progressTotal: Int,
    val progressSuccess: Int = 0,
    val progressFailed: Int = 0,
    val progressCancelled: Int = 0,
    val status: RequestStatus = RequestStatus.PENDING,
    val metadata: String? = null,
    val error: String? = null
)