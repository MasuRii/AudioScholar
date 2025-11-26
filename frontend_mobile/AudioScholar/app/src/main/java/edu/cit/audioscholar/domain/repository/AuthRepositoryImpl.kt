package edu.cit.audioscholar.domain.repository

import android.app.Application
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import edu.cit.audioscholar.R
import edu.cit.audioscholar.data.local.UserDataStore
import edu.cit.audioscholar.data.remote.dto.AuthResponse
import edu.cit.audioscholar.data.remote.dto.ChangePasswordRequest
import edu.cit.audioscholar.data.remote.dto.FirebaseTokenRequest
import edu.cit.audioscholar.data.remote.dto.GitHubCodeRequest
import edu.cit.audioscholar.data.remote.dto.LoginRequest
import edu.cit.audioscholar.data.remote.dto.RegistrationRequest
import edu.cit.audioscholar.data.remote.dto.UpdateRoleRequest
import edu.cit.audioscholar.data.remote.dto.UpdateUserProfileRequest
import edu.cit.audioscholar.data.remote.dto.UserProfileDto
import edu.cit.audioscholar.data.remote.service.ApiService
import edu.cit.audioscholar.util.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG_AUTH_REPO = "AuthRepositoryImpl"

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val application: Application,
    private val gson: Gson,
    private val userDataStore: UserDataStore,
    private val firebaseAuth: FirebaseAuth,
    private val prefs: SharedPreferences
) : AuthRepository {

    override fun saveAuthToken(token: String) {
        prefs.edit().putString("auth_token", token).apply()
        Log.d(TAG_AUTH_REPO, "Auth Token saved to SharedPreferences")
    }

    private fun clearAuthToken() {
        prefs.edit().remove("auth_token").apply()
        Log.d(TAG_AUTH_REPO, "Auth Token removed from SharedPreferences")
    }

    override suspend fun registerUser(request: RegistrationRequest): AuthResult {
        return try {
            Log.d(TAG_AUTH_REPO, "Attempting registration for email: ${request.email}")
            val response = apiService.registerUser(request)

            if (response.isSuccessful) {
                val authResponse = response.body()
                if (authResponse != null) {
                    Log.i(TAG_AUTH_REPO, "Registration successful: ${authResponse.message ?: "No message"}")
                    authResponse.token?.let { saveAuthToken(it) }
                    authResponse.customToken?.let { signInWithCustomTokenAndCheckVerification(it) }
                    Resource.Success(authResponse)
                } else {
                    Log.w(TAG_AUTH_REPO, "Registration successful (Code: ${response.code()}) but response body was null.")
                    Resource.Success(AuthResponse(success = true, message = "Registration successful"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    gson.fromJson(errorBody, AuthResponse::class.java)?.message ?: errorBody ?: "Unknown server error"
                } catch (e: Exception) {
                    errorBody ?: "Unknown server error (Code: ${response.code()})"
                }
                Log.e(TAG_AUTH_REPO, "Registration failed: ${response.code()} - $errorMessage")
                Resource.Error(errorMessage)
            }
        } catch (e: IOException) {
            Log.e(TAG_AUTH_REPO, "Network/IO exception during registration: ${e.message}", e)
            Resource.Error(application.getString(R.string.error_network_connection))
        } catch (e: HttpException) {
            Log.e(TAG_AUTH_REPO, "HTTP exception during registration: ${e.code()} - ${e.message()}", e)
            Resource.Error("HTTP Error: ${e.code()} ${e.message()}")
        } catch (e: Exception) {
            Log.e(TAG_AUTH_REPO, "Unexpected exception during registration: ${e.message}", e)
            Resource.Error(application.getString(R.string.error_unexpected_registration, e.message ?: "Unknown error"))
        }
    }

    override suspend fun loginUser(request: LoginRequest): AuthResult {
        return try {
            Log.d(TAG_AUTH_REPO, "Attempting login for email: ${request.email}")
            val response = apiService.loginUser(request)

            if (response.isSuccessful) {
                val authResponse = response.body()
                if (authResponse != null) {
                    if (authResponse.token != null) {
                        Log.i(TAG_AUTH_REPO, "Login successful. Token received.")
                        saveAuthToken(authResponse.token)
                        authResponse.customToken?.let { signInWithCustomTokenAndCheckVerification(it) }
                        Resource.Success(authResponse)
                    } else {
                        Log.w(TAG_AUTH_REPO, "Login successful (Code: ${response.code()}) but token was null in response.")
                        Resource.Error("Login successful but token missing.", authResponse)
                    }
                } else {
                    Log.w(TAG_AUTH_REPO, "Login successful (Code: ${response.code()}) but response body was null.")
                    Resource.Error("Login successful but response body was empty.")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    gson.fromJson(errorBody, AuthResponse::class.java)?.message ?: errorBody ?: "Unknown server error"
                } catch (e: Exception) {
                    errorBody ?: "Unknown server error (Code: ${response.code()})"
                }
                Log.e(TAG_AUTH_REPO, "Login failed: ${response.code()} - $errorMessage")
                Resource.Error(errorMessage)
            }
        } catch (e: IOException) {
            Log.e(TAG_AUTH_REPO, "Network/IO exception during login: ${e.message}", e)
            Resource.Error(application.getString(R.string.error_network_connection))
        } catch (e: HttpException) {
            Log.e(TAG_AUTH_REPO, "HTTP exception during login: ${e.code()} - ${e.message()}", e)
            Resource.Error("HTTP Error: ${e.code()} ${e.message()}")
        } catch (e: Exception) {
            Log.e(TAG_AUTH_REPO, "Unexpected exception during login: ${e.message}", e)
            Resource.Error(application.getString(R.string.error_unexpected_login, e.message ?: "Unknown error"))
        }
    }

    override suspend fun verifyFirebaseToken(request: FirebaseTokenRequest): AuthResult {
        return try {
            Log.d(TAG_AUTH_REPO, "Sending Firebase ID token to backend for verification.")
            val response = apiService.verifyFirebaseToken(request)

            if (response.isSuccessful) {
                val authResponse = response.body()
                if (authResponse != null && authResponse.token != null) {
                    Log.i(TAG_AUTH_REPO, "Firebase token verified successfully by backend. API JWT received.")
                    saveAuthToken(authResponse.token)
                    Resource.Success(authResponse)
                } else {
                    val errorMsg = "Backend verification successful but response or API token was null."
                    Log.w(TAG_AUTH_REPO, errorMsg)
                    Resource.Error(errorMsg, authResponse)
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    gson.fromJson(errorBody, AuthResponse::class.java)?.message ?: errorBody ?: "Unknown backend verification error"
                } catch (e: Exception) {
                    errorBody ?: "Unknown backend verification error (Code: ${response.code()})"
                }
                Log.e(TAG_AUTH_REPO, "Backend verification failed: ${response.code()} - $errorMessage")
                Resource.Error(errorMessage)
            }
        } catch (e: IOException) {
            Log.e(TAG_AUTH_REPO, "Network/IO exception during token verification call: ${e.message}", e)
            Resource.Error(application.getString(R.string.error_network_connection))
        } catch (e: HttpException) {
            Log.e(TAG_AUTH_REPO, "HTTP exception during token verification call: ${e.code()} - ${e.message()}", e)
            Resource.Error("HTTP Error: ${e.code()} ${e.message()}")
        } catch (e: Exception) {
            Log.e(TAG_AUTH_REPO, "Unexpected exception during token verification call: ${e.message}", e)
            Resource.Error(application.getString(R.string.upload_error_unexpected, e.message ?: "Unknown error"))
        }
    }

    override suspend fun verifyGoogleToken(request: FirebaseTokenRequest): AuthResult {
        return try {
            Log.d(TAG_AUTH_REPO, "Sending Google ID token to backend for verification.")
            val response = apiService.verifyGoogleToken(request)

            if (response.isSuccessful) {
                val authResponse = response.body()
                if (authResponse != null && authResponse.token != null) {
                    Log.i(TAG_AUTH_REPO, "Google token verified successfully by backend. API JWT received.")
                    saveAuthToken(authResponse.token)
                    Resource.Success(authResponse)
                } else {
                    val errorMsg = "Backend Google verification successful but response or API token was null."
                    Log.w(TAG_AUTH_REPO, errorMsg)
                    Resource.Error(errorMsg, authResponse)
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    gson.fromJson(errorBody, AuthResponse::class.java)?.message ?: errorBody ?: "Unknown backend Google verification error"
                } catch (e: Exception) {
                    errorBody ?: "Unknown backend Google verification error (Code: ${response.code()})"
                }
                Log.e(TAG_AUTH_REPO, "Backend Google verification failed: ${response.code()} - $errorMessage")
                Resource.Error(errorMessage)
            }
        } catch (e: IOException) {
            Log.e(TAG_AUTH_REPO, "Network/IO exception during Google token verification call: ${e.message}", e)
            Resource.Error(application.getString(R.string.error_network_connection))
        } catch (e: HttpException) {
            Log.e(TAG_AUTH_REPO, "HTTP exception during Google token verification call: ${e.code()} - ${e.message()}", e)
            Resource.Error("HTTP Error: ${e.code()} ${e.message()}")
        } catch (e: Exception) {
            Log.e(TAG_AUTH_REPO, "Unexpected exception during Google token verification call: ${e.message}", e)
            Resource.Error(application.getString(R.string.upload_error_unexpected, e.message ?: "Unknown error"))
        }
    }

    override suspend fun verifyGitHubCode(request: GitHubCodeRequest): AuthResult {
        return try {
            Log.d(TAG_AUTH_REPO, "Sending GitHub code to backend for verification.")
            val response = apiService.verifyGitHubCode(request)

            if (response.isSuccessful) {
                val authResponse = response.body()
                if (authResponse != null && authResponse.token != null) {
                    Log.i(TAG_AUTH_REPO, "GitHub code verified successfully by backend. API JWT received.")
                    saveAuthToken(authResponse.token)
                    Resource.Success(authResponse)
                } else {
                    val errorMsg = "Backend GitHub verification successful but response or API token was null."
                    Log.w(TAG_AUTH_REPO, errorMsg)
                    Resource.Error(errorMsg, authResponse)
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    gson.fromJson(errorBody, AuthResponse::class.java)?.message ?: errorBody ?: "Unknown backend GitHub verification error"
                } catch (e: Exception) {
                    errorBody ?: "Unknown backend GitHub verification error (Code: ${response.code()})"
                }
                Log.e(TAG_AUTH_REPO, "Backend GitHub verification failed: ${response.code()} - $errorMessage")
                Resource.Error(errorMessage)
            }
        } catch (e: IOException) {
            Log.e(TAG_AUTH_REPO, "Network/IO exception during GitHub code verification call: ${e.message}", e)
            Resource.Error(application.getString(R.string.error_network_connection))
        } catch (e: HttpException) {
            Log.e(TAG_AUTH_REPO, "HTTP exception during GitHub code verification call: ${e.code()} - ${e.message()}", e)
            Resource.Error("HTTP Error: ${e.code()} ${e.message()}")
        } catch (e: Exception) {
            Log.e(TAG_AUTH_REPO, "Unexpected exception during GitHub code verification call: ${e.message}", e)
            Resource.Error(application.getString(R.string.upload_error_unexpected, e.message ?: "Unknown error"))
        }
    }

    override fun getUserProfile(): Flow<UserProfileResult> = channelFlow {
        Log.d(TAG_AUTH_REPO, "[getUserProfile] Channel flow started.")
        var networkFetchAttempted = false
        var lastSentCacheHash: Int? = null

        val dataStoreJob = launch {
            Log.d(TAG_AUTH_REPO, "[getUserProfile] Starting DataStore collection.")
            userDataStore.userProfileFlow.collect { cachedProfile ->
                val cacheHash = cachedProfile?.hashCode()
                Log.d(TAG_AUTH_REPO, "[getUserProfile] DataStore emitted: ${if(cachedProfile != null) "Profile(hash=$cacheHash)" else "null"}")

                if (cachedProfile != null) {
                    if (cacheHash != lastSentCacheHash) {
                        Log.d(TAG_AUTH_REPO, "[getUserProfile] Sending SUCCESS from DataStore (hash=$cacheHash).")
                        send(Resource.Success(cachedProfile))
                        lastSentCacheHash = cacheHash
                    } else {
                        Log.d(TAG_AUTH_REPO, "[getUserProfile] DataStore emitted same cache hash ($cacheHash) as last sent. Skipping send.")
                    }
                } else if (!networkFetchAttempted) {
                    Log.d(TAG_AUTH_REPO, "[getUserProfile] Sending LOADING (null cache before network fetch).")
                    send(Resource.Loading(null))
                    lastSentCacheHash = null
                } else {
                    Log.d(TAG_AUTH_REPO, "[getUserProfile] DataStore emitted null after network fetch. Sending ERROR.")
                    send(Resource.Error("User profile cleared or unavailable", null))
                    lastSentCacheHash = null
                }
            }
            Log.d(TAG_AUTH_REPO, "[getUserProfile] DataStore collection finished.")
        }

        try {
            Log.d(TAG_AUTH_REPO, "[getUserProfile] Starting network fetch.")
            val response = apiService.getUserProfile()
            networkFetchAttempted = true
            Log.d(TAG_AUTH_REPO, "[getUserProfile] Network fetch finished (Code: ${response.code()}).")

            if (response.isSuccessful) {
                val networkProfile = response.body()
                if (networkProfile != null) {
                    val currentCache = userDataStore.userProfileFlow.first()
                    if (networkProfile != currentCache) {
                        Log.i(TAG_AUTH_REPO, "[getUserProfile] Network data differs from cache. Saving to DataStore (Network hash=${networkProfile.hashCode()}, Cache hash=${currentCache?.hashCode()}).")
                        userDataStore.saveUserProfile(networkProfile)
                        Log.i(TAG_AUTH_REPO, "[getUserProfile] Saved network profile to DataStore. DataStore flow should emit now.")
                    } else {
                        Log.i(TAG_AUTH_REPO, "[getUserProfile] Network data is same as cache. No DataStore update needed.")
                        if (currentCache == null) {
                            Log.d(TAG_AUTH_REPO, "[getUserProfile] Cache was null, explicitly sending network success (hash=${networkProfile.hashCode()}).")
                            if(networkProfile.hashCode() != lastSentCacheHash) {
                                send(Resource.Success(networkProfile))
                                lastSentCacheHash = networkProfile.hashCode()
                            }
                        }
                    }
                } else {
                    Log.w(TAG_AUTH_REPO, "[getUserProfile] Network success but body was null.")
                    val latestCache = userDataStore.userProfileFlow.first()
                    Log.d(TAG_AUTH_REPO, "[getUserProfile] Sending ERROR (Network null body).")
                    send(Resource.Error("Profile data missing in response.", latestCache))
                    lastSentCacheHash = latestCache?.hashCode()
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try { gson.fromJson(errorBody, AuthResponse::class.java)?.message ?: errorBody ?: "Unknown server error" } catch (e: Exception) { errorBody ?: "Unknown server error (Code: ${response.code()})" }
                Log.e(TAG_AUTH_REPO, "[getUserProfile] Network fetch failed: ${response.code()} - $errorMessage")
                val finalMessage = if (response.code() == 401 || response.code() == 403) { application.getString(R.string.error_unauthorized) } else { errorMessage }
                val latestCache = userDataStore.userProfileFlow.first()
                Log.d(TAG_AUTH_REPO, "[getUserProfile] Sending ERROR (Network ${response.code()}).")
                send(Resource.Error(finalMessage, latestCache))
                lastSentCacheHash = latestCache?.hashCode()
            }
        } catch (e: CancellationException) {
            Log.w(TAG_AUTH_REPO, "[getUserProfile] Network fetch cancelled.")
            throw e
        } catch (e: Exception) {
            networkFetchAttempted = true
            Log.e(TAG_AUTH_REPO, "[getUserProfile] Network fetch exception: ${e::class.java.simpleName} - ${e.message}", e)
            val latestCache = userDataStore.userProfileFlow.first()
            val errorMsg = when(e) {
                is IOException -> application.getString(R.string.error_network_connection)
                is HttpException -> if (e.code() == 401 || e.code() == 403) application.getString(R.string.error_unauthorized) else "HTTP Error: ${e.code()} ${e.message()}"
                else -> application.getString(R.string.error_unexpected_profile_fetch, e.message ?: "Unknown error")
            }
            Log.d(TAG_AUTH_REPO, "[getUserProfile] Sending ERROR (Network Exception).")
            send(Resource.Error(errorMsg, latestCache))
            lastSentCacheHash = latestCache?.hashCode()
        }

        awaitClose {
            Log.d(TAG_AUTH_REPO, "[getUserProfile] Channel flow closing. Cancelling DataStore observer.")
            dataStoreJob.cancel()
        }
    }


    override suspend fun updateUserProfile(request: UpdateUserProfileRequest): UserProfileResult {
        val result = try {
            Log.d(TAG_AUTH_REPO, "Attempting to update user profile with display name: ${request.displayName ?: "Not Provided"}")
            val response = apiService.updateUserProfile(request)

            if (response.isSuccessful) {
                val updatedProfile = response.body()
                if (updatedProfile != null) {
                    Log.i(TAG_AUTH_REPO, "User profile updated successfully. Email: ${updatedProfile.email}")
                    Log.d(TAG_AUTH_REPO, "[updateUserProfile] Saving updated profile to DataStore (hash=${updatedProfile.hashCode()}).")
                    userDataStore.saveUserProfile(updatedProfile)
                    Resource.Success(updatedProfile)
                } else {
                    Log.w(TAG_AUTH_REPO, "Update profile successful (Code: ${response.code()}) but response body was null.")
                    Resource.Success(UserProfileDto(null, null, null, null, null, null))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    gson.fromJson(errorBody, AuthResponse::class.java)?.message ?: errorBody ?: "Unknown server error" } catch (e: Exception) { errorBody ?: "Unknown server error (Code: ${response.code()})" }
                Log.e(TAG_AUTH_REPO, "Update profile failed: ${response.code()} - $errorMessage")
                val finalMessage = if (response.code() == 401 || response.code() == 403) {
                    application.getString(R.string.error_unauthorized) } else if (response.code() == 400) { "Update failed: Invalid data. ${errorMessage ?: ""}".trim() } else { errorMessage }
                Resource.Error(finalMessage)
            }
        } catch (e: IOException) {
            Log.e(TAG_AUTH_REPO, "Network/IO exception during update profile: ${e.message}", e); Resource.Error(application.getString(R.string.error_network_connection))
        } catch (e: HttpException) {
            Log.e(TAG_AUTH_REPO, "HTTP exception during update profile: ${e.code()} - ${e.message()}", e); val finalMessage = if (e.code() == 401 || e.code() == 403) { application.getString(R.string.error_unauthorized) } else { "HTTP Error: ${e.code()} ${e.message()}" }; Resource.Error(finalMessage)
        } catch (e: Exception) {
            Log.e(TAG_AUTH_REPO, "Unexpected exception during update profile: ${e.message}", e); Resource.Error(application.getString(R.string.error_unexpected_profile_update, e.message ?: "Unknown error"))
        }
        return result
    }


    override suspend fun uploadAvatar(imageUri: Uri): UserProfileResult {
        val contentResolver = application.contentResolver
        var fileName = "avatar_upload"
        var fileSize: Long? = null

        contentResolver.query(imageUri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            if (nameIndex != -1) {
                fileName = cursor.getString(nameIndex) ?: fileName
            }
            if (sizeIndex != -1) {
                fileSize = cursor.getLong(sizeIndex)
            }
        }

        val mimeType = contentResolver.getType(imageUri)
        if (mimeType == null) {
            Log.e(TAG_AUTH_REPO, "Could not determine MIME type for avatar URI: $imageUri")
            return Resource.Error("Could not determine file type for selected image.")
        }

        Log.d(TAG_AUTH_REPO, "Preparing avatar upload. URI: $imageUri, Name: $fileName, Type: $mimeType, Size: $fileSize")


        val result = try {
            val requestBody = object : RequestBody() {
                override fun contentType() = mimeType.toMediaTypeOrNull()
                override fun contentLength(): Long = fileSize ?: -1
                override fun writeTo(sink: BufferedSink) {
                    contentResolver.openInputStream(imageUri)?.use { inputStream ->
                        sink.writeAll(inputStream.source())
                    } ?: throw IOException("Could not open input stream for URI: $imageUri")
                }
            }
            val bodyPart = MultipartBody.Part.createFormData("avatar", fileName, requestBody)

            Log.d(TAG_AUTH_REPO, "Attempting to upload avatar via API service.")
            val response = apiService.uploadAvatar(bodyPart)

            if (response.isSuccessful) {
                val updatedProfile = response.body()
                if (updatedProfile != null) {
                    Log.i(TAG_AUTH_REPO, "Avatar uploaded successfully. New profile URL: ${updatedProfile.profileImageUrl}")
                    Log.d(TAG_AUTH_REPO, "[uploadAvatar] Saving updated profile to DataStore (hash=${updatedProfile.hashCode()}).")
                    userDataStore.saveUserProfile(updatedProfile)
                    Resource.Success(updatedProfile)
                } else {
                    Log.w(TAG_AUTH_REPO, "Avatar upload successful (Code: ${response.code()}) but response body was null.")
                    Resource.Error("Avatar upload succeeded but failed to get updated profile data.")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    gson.fromJson(errorBody, AuthResponse::class.java)?.message ?: gson.fromJson(errorBody, UserProfileDto::class.java)?.displayName ?: errorBody ?: "Unknown server error during avatar upload" } catch (e: Exception) { errorBody ?: "Unknown server error (Code: ${response.code()})" }
                Log.e(TAG_AUTH_REPO, "Avatar upload failed: ${response.code()} - $errorMessage")
                val finalMessage = if (response.code() == 401 || response.code() == 403) {
                    application.getString(R.string.error_unauthorized) } else if (response.code() == 413) { application.getString(R.string.upload_error_size_exceeded_avatar) } else if (response.code() == 415) { application.getString(R.string.upload_error_unsupported_format_avatar) } else { "Avatar upload failed: $errorMessage" }
                Resource.Error(finalMessage)
            }
        } catch (e: IOException) {
            Log.e(TAG_AUTH_REPO, "IOException during avatar upload preparation/call: ${e.message}", e); Resource.Error(application.getString(R.string.error_unexpected, e.message ?: "Could not read image file"))
        } catch (e: HttpException) {
            Log.e(TAG_AUTH_REPO, "HTTP exception during avatar upload: ${e.code()} - ${e.message()}", e); val finalMessage = if (e.code() == 401 || e.code() == 403) { application.getString(R.string.error_unauthorized) } else { "HTTP Error during avatar upload: ${e.code()} ${e.message()}" }; Resource.Error(finalMessage)
        } catch (e: Exception) {
            Log.e(TAG_AUTH_REPO, "Unexpected exception during avatar upload: ${e.message}", e); Resource.Error(application.getString(R.string.upload_error_unexpected, e.message ?: "Unknown error"))
        }
        return result
    }


    override suspend fun changePassword(request: ChangePasswordRequest): SimpleResult {
        return try {
            Log.d(TAG_AUTH_REPO, "Attempting to change password.")
            val response = apiService.changePassword(request)

            if (response.isSuccessful) {
                Log.i(TAG_AUTH_REPO, "Password changed successfully via API.")
                Resource.Success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    gson.fromJson(errorBody, AuthResponse::class.java)?.message ?: errorBody ?: "Unknown server error"
                } catch (e: Exception) {
                    errorBody ?: "Unknown server error (Code: ${response.code()})"
                }
                Log.e(TAG_AUTH_REPO, "Change password failed: ${response.code()} - $errorMessage")
                when (response.code()) {
                    400 -> Resource.Error(errorMessage ?: application.getString(R.string.error_change_password_invalid_current))
                    401, 403 -> Resource.Error(application.getString(R.string.error_unauthorized))
                    else -> Resource.Error(errorMessage ?: "Failed to change password.")
                }
            }
        } catch (e: IOException) {
            Log.e(TAG_AUTH_REPO, "Network/IO exception during change password: ${e.message}", e)
            Resource.Error(application.getString(R.string.error_network_connection))
        } catch (e: HttpException) {
            Log.e(TAG_AUTH_REPO, "HTTP exception during change password: ${e.code()} - ${e.message()}", e)
            if (e.code() == 401 || e.code() == 403) {
                Resource.Error(application.getString(R.string.error_unauthorized))
            } else {
                Resource.Error("HTTP Error: ${e.code()} ${e.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG_AUTH_REPO, "Unexpected exception during change password: ${e.message}", e)
            Resource.Error(application.getString(R.string.error_unexpected_change_password, e.message ?: "Unknown error"))
        }
    }

    override fun sendPasswordResetEmail(email: String): Flow<SimpleResult> = flow {
        emit(Resource.Loading())
        try {
            Log.d(TAG_AUTH_REPO, "Sending password reset email to: $email")
            val actionCodeSettings = ActionCodeSettings.newBuilder()
                .setUrl("https://audioscholar-39b22.web.app/resetPassword")
                .setHandleCodeInApp(true)
                .setAndroidPackageName(
                    "edu.cit.audioscholar",
                    true,
                    null
                )
                .build()

            firebaseAuth.sendPasswordResetEmail(email, actionCodeSettings).await()
            Log.i(TAG_AUTH_REPO, "Password reset email sent successfully.")
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            Log.e(TAG_AUTH_REPO, "Error sending password reset email: ${e.message}", e)
            emit(Resource.Error(e.message ?: "Failed to send password reset email."))
        }
    }

    override suspend fun applyActionCode(code: String): SimpleResult {
        return try {
            Log.d(TAG_AUTH_REPO, "Applying action code: $code")
            firebaseAuth.applyActionCode(code).await()
            Log.i(TAG_AUTH_REPO, "Action code applied successfully.")
            // If it was email verification, reload user to update emailVerified status
            firebaseAuth.currentUser?.reload()?.await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG_AUTH_REPO, "Failed to apply action code: ${e.message}", e)
            Resource.Error(e.message ?: "Failed to verify code.")
        }
    }

    override suspend fun verifyPasswordResetCode(code: String): Resource<String> {
        return try {
            Log.d(TAG_AUTH_REPO, "Verifying password reset code: $code")
            val email = firebaseAuth.verifyPasswordResetCode(code).await()
            Log.i(TAG_AUTH_REPO, "Password reset code valid for email: $email")
            Resource.Success(email ?: "")
        } catch (e: Exception) {
            Log.e(TAG_AUTH_REPO, "Invalid password reset code: ${e.message}", e)
            Resource.Error(e.message ?: "Invalid code.")
        }
    }

    override suspend fun confirmPasswordReset(code: String, newPassword: String): SimpleResult {
        return try {
            Log.d(TAG_AUTH_REPO, "Confirming password reset with code.")
            firebaseAuth.confirmPasswordReset(code, newPassword).await()
            Log.i(TAG_AUTH_REPO, "Password reset confirmed successfully.")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG_AUTH_REPO, "Failed to confirm password reset: ${e.message}", e)
            Resource.Error(e.message ?: "Failed to reset password.")
        }
    }

    override suspend fun logout(): SimpleResult {
        return try {
            Log.d(TAG_AUTH_REPO, "Attempting API logout call.")
            val response = apiService.logout()

            if (response.isSuccessful) {
                Log.i(TAG_AUTH_REPO, "API logout successful (Code: ${response.code()}).")
                clearAuthToken()
                Resource.Success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    gson.fromJson(errorBody, AuthResponse::class.java)?.message ?: errorBody ?: "Unknown server error during logout"
                } catch (e: Exception) {
                    errorBody ?: "Unknown server error (Code: ${response.code()})"
                }
                Log.w(TAG_AUTH_REPO, "API logout failed (Code: ${response.code()}): $errorMessage")
                Resource.Error("API Logout Failed: ${response.code()} - $errorMessage")
            }
        } catch (e: IOException) {
            Log.e(TAG_AUTH_REPO, "Network/IO exception during logout: ${e.message}", e)
            Resource.Error(application.getString(R.string.error_network_connection))
        } catch (e: HttpException) {
            Log.e(TAG_AUTH_REPO, "HTTP exception during logout: ${e.code()} - ${e.message()}", e)
            Resource.Error("HTTP Error during logout: ${e.code()} ${e.message()}")
        } catch (e: Exception) {
            Log.e(TAG_AUTH_REPO, "Unexpected exception during logout: ${e.message}", e)
            Resource.Error(application.getString(R.string.error_unexpected, e.message ?: "Unknown error"))
        }
    }

    override suspend fun clearLocalUserCache() {
        Log.d(TAG_AUTH_REPO, "Clearing user profile from DataStore.")
        userDataStore.clearUserProfile()
    }

    override suspend fun updateUserRole(userId: String, role: String): SimpleResult {
        return try {
            Log.d(TAG_AUTH_REPO, "Updating user role to: $role for user: $userId")
            val response = apiService.updateUserRole(userId, UpdateRoleRequest(role))

            if (response.isSuccessful) {
                Log.i(TAG_AUTH_REPO, "User role updated successfully to: $role")
                // Refresh user profile to reflect the new role locally
                val profileResponse = apiService.getUserProfile()
                if (profileResponse.isSuccessful && profileResponse.body() != null) {
                    userDataStore.saveUserProfile(profileResponse.body()!!)
                    Log.d(TAG_AUTH_REPO, "User profile refreshed after role update.")
                } else {
                    Log.w(TAG_AUTH_REPO, "Failed to refresh user profile after role update.")
                }
                Resource.Success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    gson.fromJson(errorBody, AuthResponse::class.java)?.message ?: errorBody ?: "Unknown server error"
                } catch (e: Exception) {
                    errorBody ?: "Unknown server error (Code: ${response.code()})"
                }
                Log.e(TAG_AUTH_REPO, "Role update failed: ${response.code()} - $errorMessage")
                Resource.Error(errorMessage)
            }
        } catch (e: IOException) {
            Log.e(TAG_AUTH_REPO, "Network/IO exception during role update: ${e.message}", e)
            Resource.Error(application.getString(R.string.error_network_connection))
        } catch (e: HttpException) {
            Log.e(TAG_AUTH_REPO, "HTTP exception during role update: ${e.code()} - ${e.message()}", e)
            Resource.Error("HTTP Error: ${e.code()} ${e.message()}")
        } catch (e: Exception) {
            Log.e(TAG_AUTH_REPO, "Unexpected exception during role update: ${e.message}", e)
            Resource.Error(application.getString(R.string.error_unexpected, e.message ?: "Unknown error"))
        }
    }

    private suspend fun signInWithCustomTokenAndCheckVerification(customToken: String?) {
        if (customToken.isNullOrBlank()) {
            Log.w(TAG_AUTH_REPO, "signInWithCustomToken was called with a null or blank token.")
            return
        }

        try {
            Log.d(TAG_AUTH_REPO, "Initiating Firebase sign-in with custom token...")
            val authResult = firebaseAuth.signInWithCustomToken(customToken).await()
            val user = authResult.user

            if (user == null) {
                Log.w(TAG_AUTH_REPO, "Firebase sign-in with custom token succeeded but the user object is null.")
                return
            }

            Log.i(TAG_AUTH_REPO, "Firebase sign-in successful for user: ${user.uid}")

            if (!user.isEmailVerified) {
                Log.i(TAG_AUTH_REPO, "User email is not verified for UID: ${user.uid}. Sending verification email.")
                val actionCodeSettings = ActionCodeSettings.newBuilder()
                    .setUrl("https://audioscholar-39b22.web.app/verify?email=${user.email}")
                    .setHandleCodeInApp(true)
                    .setAndroidPackageName("edu.cit.audioscholar", true, null)
                    .build()
                
                user.sendEmailVerification(actionCodeSettings).await()
                Log.i(TAG_AUTH_REPO, "Verification email sent successfully to ${user.email}.")
            } else {
                Log.i(TAG_AUTH_REPO, "User email is already verified for UID: ${user.uid}.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG_AUTH_REPO, "Failed to sign in with custom token or send verification email.", e)
        }
    }
}