package edu.cit.audioscholar.ui.auth

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.cit.audioscholar.domain.repository.AuthRepository
import edu.cit.audioscholar.ui.main.SplashActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class EmailVerificationUiState(
    val isLoading: Boolean = false,
    val isVerified: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val resendEnabled: Boolean = true
)

sealed class EmailVerificationEvent {
    object NavigateToHome : EmailVerificationEvent()
    object NavigateToLogin : EmailVerificationEvent()
}

@HiltViewModel
class EmailVerificationViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val prefs: SharedPreferences,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmailVerificationUiState())
    val uiState: StateFlow<EmailVerificationUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<EmailVerificationEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private var pollingJob: Job? = null

    companion object {
        private const val TAG = "EmailVerificationViewModel"
    }

    init {
        // Start polling for email verification status
        if (firebaseAuth.currentUser == null) {
            Log.w(TAG, "Init: User is null, redirecting to login.")
            viewModelScope.launch {
                _eventFlow.emit(EmailVerificationEvent.NavigateToLogin)
            }
        } else {
            startEmailVerificationPolling()
        }
    }

    fun getCurrentUserEmail(): String {
        return firebaseAuth.currentUser?.email ?: ""
    }

    private fun startEmailVerificationPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                checkEmailVerificationStatus(isPolling = true)
                delay(10000) // Poll every 10 seconds
            }
        }
    }

    fun checkEmailVerificationStatus(isPolling: Boolean = false) {
        viewModelScope.launch {
            if (!isPolling) _uiState.update { it.copy(isLoading = true) }
            
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                _eventFlow.emit(EmailVerificationEvent.NavigateToLogin)
                return@launch
            }

            try {
                // Reload user to get the latest status
                currentUser.reload().await()
                
                val isVerified = currentUser.isEmailVerified
                Log.d(TAG, "Email verification status: $isVerified")

                if (isVerified) {
                    pollingJob?.cancel()
                    _uiState.update { it.copy(isVerified = true, isLoading = false) }
                } else {
                     if (!isPolling) _uiState.update { it.copy(isLoading = false, errorMessage = "Email not verified yet.") }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking verification status", e)
                if (!isPolling) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to check status") }
                }
            }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, resendEnabled = false) }
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                 _eventFlow.emit(EmailVerificationEvent.NavigateToLogin)
                 return@launch
            }

            try {
                val actionCodeSettings = ActionCodeSettings.newBuilder()
                    .setUrl("https://audioscholar-39b22.web.app/verify?email=${currentUser.email}")
                    .setHandleCodeInApp(true)
                    .setAndroidPackageName("edu.cit.audioscholar", true, null)
                    .build()

                currentUser.sendEmailVerification(actionCodeSettings).await()
                _uiState.update { it.copy(isLoading = false, infoMessage = "Verification email sent!") }
                
                // Re-enable button after delay
                delay(60000) 
                _uiState.update { it.copy(resendEnabled = true) }

            } catch (e: Exception) {
                Log.e(TAG, "Error sending verification email", e)
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to send email", resendEnabled = true) }
            }
        }
    }
    
    fun navigateToLogin() {
        viewModelScope.launch {
            performLogoutCleanup()
            _eventFlow.emit(EmailVerificationEvent.NavigateToLogin)
        }
    }

    private suspend fun performLogoutCleanup() {
        firebaseAuth.signOut()
        authRepository.clearLocalUserCache()
        with(prefs.edit()) {
            remove(LoginViewModel.KEY_AUTH_TOKEN)
            putBoolean(SplashActivity.KEY_IS_LOGGED_IN, false)
            apply()
        }
    }

    fun consumeErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    fun consumeInfoMessage() {
         _uiState.update { it.copy(infoMessage = null) }
    }
}