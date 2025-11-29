package edu.cit.audioscholar.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UserNoteDto(
    @SerializedName("noteId")
    val id: String? = null,
    val userId: String? = null,
    val recordingId: String,
    val content: String,
    val tags: List<String>? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)