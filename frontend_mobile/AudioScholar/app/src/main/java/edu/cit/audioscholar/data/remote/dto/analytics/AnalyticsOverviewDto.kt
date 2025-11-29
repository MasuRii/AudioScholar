package edu.cit.audioscholar.data.remote.dto.analytics

data class AnalyticsOverviewDto(
    val totalUsers: Long,
    val totalRecordings: Long,
    val totalStorageBytes: Long,
    val totalDurationSeconds: Long
)