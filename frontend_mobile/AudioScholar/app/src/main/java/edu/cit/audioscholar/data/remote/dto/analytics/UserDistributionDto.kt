package edu.cit.audioscholar.data.remote.dto.analytics

data class UserDistributionDto(
    val usersByProvider: Map<String, Long>,
    val usersByRole: Map<String, Long>
)