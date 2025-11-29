package edu.cit.audioscholar.data.remote.dto.analytics

data class ContentEngagementDto(
    val recordingId: String,
    val title: String,
    val favoriteCount: Int
)