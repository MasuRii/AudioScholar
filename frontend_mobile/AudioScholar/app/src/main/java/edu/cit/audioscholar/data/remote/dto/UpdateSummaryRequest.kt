package edu.cit.audioscholar.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UpdateSummaryRequest(
    @SerializedName("formattedSummaryText")
    val formattedSummaryText: String? = null,

    @SerializedName("keyPoints")
    val keyPoints: List<String>? = null,

    @SerializedName("topics")
    val topics: List<String>? = null,

    @SerializedName("glossary")
    val glossary: List<GlossaryItemDto>? = null
)