package edu.cit.audioscholar.domain.repository

import edu.cit.audioscholar.data.remote.dto.*
import kotlinx.coroutines.flow.Flow
import java.io.File

sealed class UploadResult {
    data class Success(val metadata: AudioMetadataDto?) : UploadResult()
    data class Error(val message: String) : UploadResult()
    data class Progress(val percentage: Int) : UploadResult()
    data object Loading : UploadResult()
}

interface RemoteAudioRepository {
    fun uploadAudioFile(
        audioFile: File,
        powerpointFile: File? = null,
        title: String?,
        description: String?
    ): Flow<UploadResult>

    fun getCloudRecordings(): Flow<Result<List<AudioMetadataDto>>>

    fun getSummary(recordingId: String): Flow<Result<SummaryResponseDto>>

    fun getRecommendations(recordingId: String): Flow<Result<List<RecommendationDto>>>

    fun getCloudRecordingDetails(recordingId: String): Flow<Result<AudioMetadataDto>>

    fun updateRecordingDetails(recordingId: String, title: String?, description: String?): Flow<Result<AudioMetadataDto>>

    fun updateSummary(
        summaryId: String,
        newContent: String,
        keyPoints: List<String>?,
        topics: List<String>?,
        glossary: List<GlossaryItemDto>?
    ): Flow<Result<SummaryResponseDto>>

    fun dismissRecommendation(recommendationId: String): Flow<Result<Unit>>

    fun deleteCloudRecording(metadataId: String): Flow<Result<Unit>>

    fun createNote(recordingId: String, content: String, tags: List<String>?): Flow<Result<UserNoteDto>>

    fun getNotes(recordingId: String): Flow<Result<List<UserNoteDto>>>

    fun updateNote(noteId: String, content: String?, tags: List<String>?): Flow<Result<UserNoteDto>>

    fun deleteNote(noteId: String): Flow<Result<Unit>>

}