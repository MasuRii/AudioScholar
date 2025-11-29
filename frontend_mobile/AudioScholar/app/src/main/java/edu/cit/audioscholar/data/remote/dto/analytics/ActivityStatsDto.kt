package edu.cit.audioscholar.data.remote.dto.analytics

data class ActivityStatsDto(
    val newUsersLast30Days: Map<String, Long>,
    val newRecordingsLast30Days: Map<String, Long>
)