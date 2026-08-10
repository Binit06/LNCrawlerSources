package com.halovoid.lncrawler.domain.models

data class Artifact(
    val id: Int,
    val novelUrl: String,
    val requestId: String,
    val artifactDestination: String,
    val artifactName: String
)