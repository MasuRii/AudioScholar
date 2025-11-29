package edu.cit.audioscholar.data.remote.dto

data class UpdateUserNoteRequest(
    val content: String? = null,
    val tags: List<String>? = null
)