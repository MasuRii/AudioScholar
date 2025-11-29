package edu.cit.audioscholar.data.remote.dto

data class CreateUserNoteRequest(
    val recordingId: String,
    val content: String,
    val tags: List<String>? = null
)