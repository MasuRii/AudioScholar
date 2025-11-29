package edu.cit.audioscholar.data.remote.service

import edu.cit.audioscholar.data.remote.dto.*
import edu.cit.audioscholar.data.remote.dto.admin.*
import edu.cit.audioscholar.data.remote.dto.analytics.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @Multipart
    @POST("/api/audio/upload")
    suspend fun uploadAudio(
        @Part file: MultipartBody.Part,
        @Part powerpointFile: MultipartBody.Part?,
        @Part("title") title: RequestBody?,
        @Part("description") description: RequestBody?
    ): Response<AudioMetadataDto>

    @Headers("Cache-Control: no-cache")
    @GET("/api/audio/metadata")
    suspend fun getAudioMetadataList(): Response<List<AudioMetadataDto>>

    @DELETE("/api/audio/metadata/{id}")
    suspend fun deleteAudioMetadata(
        @Path("id") metadataId: String
    ): Response<Unit>

    @POST("/api/auth/register")
    suspend fun registerUser(
        @Body registrationRequest: RegistrationRequest
    ): Response<AuthResponse>

    @POST("/api/auth/login")
    suspend fun loginUser(
        @Body loginRequest: LoginRequest
    ): Response<AuthResponse>

    @POST("/api/auth/verify-firebase-token")
    suspend fun verifyFirebaseToken(
        @Body tokenRequest: FirebaseTokenRequest
    ): Response<AuthResponse>

    @POST("/api/auth/verify-google-token")
    suspend fun verifyGoogleToken(
        @Body tokenRequest: FirebaseTokenRequest
    ): Response<AuthResponse>

    @POST("/api/auth/verify-github-code")
    suspend fun verifyGitHubCode(
        @Body codeRequest: GitHubCodeRequest
    ): Response<AuthResponse>

    @GET("/api/users/me")
    suspend fun getUserProfile(): Response<UserProfileDto>

    @PUT("/api/users/me")
    suspend fun updateUserProfile(
        @Body updateUserProfileRequest: UpdateUserProfileRequest
    ): Response<UserProfileDto>

    @Multipart
    @POST("/api/users/me/avatar")
    suspend fun uploadAvatar(
        @Part avatar: MultipartBody.Part
    ): Response<UserProfileDto>

    @POST("/api/auth/change-password")
    suspend fun changePassword(
        @Body changePasswordRequest: ChangePasswordRequest
    ): Response<AuthResponse>

    @POST("/api/auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("/api/recordings/{recordingId}/summary")
    suspend fun getRecordingSummary(
        @Path("recordingId") recordingId: String
    ): Response<SummaryResponseDto>

    @GET("/api/v1/recommendations/recording/{recordingId}")
    suspend fun getRecordingRecommendations(
        @Path("recordingId") recordingId: String
    ): Response<List<RecommendationDto>>

    @GET("/api/audio/recordings/{recordingId}")
    suspend fun getRecordingDetails(
        @Path("recordingId") recordingId: String
    ): Response<AudioMetadataDto>

    @PATCH("/api/audio/recordings/{recordingId}")
    suspend fun updateRecordingDetails(
        @Path("recordingId") recordingId: String,
        @Body request: UpdateRecordingRequest
    ): Response<AudioMetadataDto>

    @PATCH("/api/summaries/{summaryId}")
    suspend fun updateSummary(
        @Path("summaryId") summaryId: String,
        @Body request: UpdateSummaryRequest
    ): Response<SummaryResponseDto>

    @DELETE("/api/v1/recommendations/{id}")
    suspend fun dismissRecommendation(
        @Path("id") recommendationId: String
    ): Response<Unit>

    @POST("/api/users/me/fcm-token")
    suspend fun registerFcmToken(
        @Body fcmTokenRequest: FcmTokenRequestDto
    ): Response<Unit>

    @PUT("/api/users/{userId}/role")
    suspend fun updateUserRole(
        @Path("userId") userId: String,
        @Body roleRequest: UpdateRoleRequest
    ): Response<Unit>

    @POST("/api/notes")
    suspend fun createNote(
        @Body request: CreateUserNoteRequest
    ): Response<UserNoteDto>

    @GET("/api/notes")
    suspend fun getNotes(
        @Query("recordingId") recordingId: String
    ): Response<List<UserNoteDto>>

    @PATCH("/api/notes/{id}")
    suspend fun updateNote(
        @Path("id") noteId: String,
        @Body request: UpdateUserNoteRequest
    ): Response<UserNoteDto>

    @DELETE("/api/notes/{id}")
    suspend fun deleteNote(
        @Path("id") noteId: String
    ): Response<Unit>

    // Admin & Analytics Endpoints

    @GET("/api/admin/users")
    suspend fun getUsers(
        @Query("limit") limit: Int = 20,
        @Query("pageToken") pageToken: String? = null
    ): Response<AdminUserListResponse>

    @PUT("/api/admin/users/{uid}/status")
    suspend fun updateUserStatus(
        @Path("uid") uid: String,
        @Body request: AdminUpdateUserStatusRequest
    ): Response<Map<String, Any>>

    @PUT("/api/admin/users/{uid}/roles")
    suspend fun updateUserRoles(
        @Path("uid") uid: String,
        @Body request: AdminUpdateUserRolesRequest
    ): Response<Map<String, Any>>

    @GET("/api/admin/analytics/overview")
    suspend fun getAnalyticsOverview(): Response<AnalyticsOverviewDto>

    @GET("/api/admin/analytics/activity")
    suspend fun getActivityStats(): Response<ActivityStatsDto>

    @GET("/api/admin/analytics/users/distribution")
    suspend fun getUserDistribution(): Response<UserDistributionDto>

    @GET("/api/admin/analytics/content/engagement")
    suspend fun getContentEngagement(): Response<List<ContentEngagementDto>>

}