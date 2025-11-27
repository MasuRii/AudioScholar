package edu.cit.audioscholar.ui.profile

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.cit.audioscholar.domain.repository.AuthRepository
import edu.cit.audioscholar.ui.auth.LoginViewModel
import edu.cit.audioscholar.ui.main.SplashActivity
import edu.cit.audioscholar.util.PremiumStatusManager
import edu.cit.audioscholar.util.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileContentState {
    data object Loading : ProfileContentState
    data class Error(val message: String) : ProfileContentState
    data class Success(
        val name: String,
        val email: String,
        val profileImageUrl: String?,
        val isPremium: Boolean,
        val hasPasswordProvider: Boolean
    ) : ProfileContentState
}

data class UserProfileUiState(
    val contentState: ProfileContentState = ProfileContentState.Loading,
    val navigateToLogin: Boolean = false,
    val userMessage: String? = null
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val prefs: SharedPreferences,
    private val premiumStatusManager: PremiumStatusManager,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    private var loadProfileJob: Job? = null

    init {
        loadUserProfile()
        observePremiumStatus()
    }

    private fun observePremiumStatus() {
        viewModelScope.launch {
            premiumStatusManager.isPremiumUserFlow.collect { isPremium ->
                val currentContent = _uiState.value.contentState
                if (currentContent is ProfileContentState.Success) {
                    _uiState.update {
                        it.copy(
                            contentState = currentContent.copy(isPremium = isPremium)
                        )
                    }
                }
            }
        }
    }

    fun loadUserProfile() {
        loadProfileJob?.cancel()

        loadProfileJob = viewModelScope.launch {
            Log.d("UserProfileViewModel", "Starting to collect user profile flow.")
            authRepository.getUserProfile()
                .onStart {
                    Log.d("UserProfileViewModel", "Flow started.")
                    if (_uiState.value.contentState !is ProfileContentState.Success) {
                         _uiState.update { it.copy(contentState = ProfileContentState.Loading) }
                    }
                }
                .catch { e ->
                    Log.e("UserProfileViewModel", "Error collecting profile flow: ${e.message}", e)
                    _uiState.update {
                        it.copy(
                            contentState = ProfileContentState.Error("An unexpected error occurred: ${e.message}")
                        )
                    }
                }
                .collect { result ->
                    Log.d("UserProfileViewModel", "Received profile result: ${result::class.simpleName}")
                    when (result) {
                        is Resource.Success -> {
                            val profileData = result.data
                            if (profileData != null) {
                                Log.i("UserProfileViewModel", "Profile loaded: ${profileData.displayName}")
                                premiumStatusManager.updatePremiumStatus(profileData)
                                val isPremium = premiumStatusManager.isPremiumUser()
                                val hasPasswordProvider = firebaseAuth.currentUser?.providerData?.any {
                                    it.providerId == EmailAuthProvider.PROVIDER_ID
                                } ?: false

                                _uiState.update {
                                    it.copy(
                                        contentState = ProfileContentState.Success(
                                            name = profileData.displayName ?: "",
                                            email = profileData.email ?: "",
                                            profileImageUrl = profileData.profileImageUrl,
                                            isPremium = isPremium,
                                            hasPasswordProvider = hasPasswordProvider
                                        )
                                    )
                                }
                            } else {
                                _uiState.update {
                                    it.copy(contentState = ProfileContentState.Error("Failed to retrieve profile details."))
                                }
                            }
                        }
                        is Resource.Error -> {
                            Log.e("UserProfileViewModel", "Error loading profile: ${result.message}")
                            val current = _uiState.value.contentState
                            if (current is ProfileContentState.Success) {
                                _uiState.update { it.copy(userMessage = result.message ?: "Error updating profile") }
                            } else {
                                _uiState.update { 
                                    it.copy(contentState = ProfileContentState.Error(result.message ?: "Failed to retrieve profile details.")) 
                                }
                            }
                        }
                        is Resource.Loading -> {
                            val cachedData = result.data
                            if (cachedData != null) {
                                val isPremium = if (cachedData.roles?.contains("ROLE_PREMIUM") == true) {
                                    true
                                } else {
                                    premiumStatusManager.isPremiumUser()
                                }
                                val hasPasswordProvider = firebaseAuth.currentUser?.providerData?.any {
                                    it.providerId == EmailAuthProvider.PROVIDER_ID
                                } ?: false

                                _uiState.update {
                                    it.copy(
                                        contentState = ProfileContentState.Success(
                                            name = cachedData.displayName ?: "",
                                            email = cachedData.email ?: "",
                                            profileImageUrl = cachedData.profileImageUrl,
                                            isPremium = isPremium,
                                            hasPasswordProvider = hasPasswordProvider
                                        )
                                    )
                                }
                            } else {
                                if (_uiState.value.contentState !is ProfileContentState.Success) {
                                    _uiState.update { it.copy(contentState = ProfileContentState.Loading) }
                                }
                            }
                        }
                    }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            clearSessionAndNavigateToLogin()
        }
    }

    private fun clearSessionAndNavigateToLogin() {
        viewModelScope.launch {
            authRepository.clearLocalUserCache()
            premiumStatusManager.clearPremiumStatus()

            with(prefs.edit()) {
                remove(LoginViewModel.KEY_AUTH_TOKEN)
                putBoolean(SplashActivity.KEY_IS_LOGGED_IN, false)
                apply()
            }
            _uiState.update { UserProfileUiState(navigateToLogin = true) }
        }
    }

    fun onLoginNavigationComplete() {
        _uiState.update { it.copy(navigateToLogin = false) }
    }

    fun consumeErrorMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        loadProfileJob?.cancel()
    }
}