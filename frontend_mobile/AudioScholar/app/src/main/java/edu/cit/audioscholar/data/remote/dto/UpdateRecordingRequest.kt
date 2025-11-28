package edu.cit.audioscholar.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UpdateRecordingRequest(
    @SerializedName("title")
    val title: String? = null,

    @SerializedName("description")
    val description: String? = null
)