package edu.cit.audioscholar.ui.auth

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import edu.cit.audioscholar.BuildConfig
import edu.cit.audioscholar.R
import edu.cit.audioscholar.data.remote.dto.FirebaseTokenRequest
import edu.cit.audioscholar.data.remote.dto.GitHubCodeRequest
import edu.cit.audioscholar.domain.repository.AuthRepository
import edu.cit.audioscholar.domain.repository.NotificationRepository
import edu.cit.audioscholar.di.ApplicationScope
import edu.cit.audioscholar.ui.main.SplashActivity
import edu.cit.audioscholar.util.Resource
import edu.cit.audioscholar.util.FcmTokenProvider
import edu.cit.audioscholar.util.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

enum class WelcomeMessageType {
    NEW_AFTER_ONBOARDING,
    RETURNING
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isEmailLoginLoading: Boolean = false,
    val isGoogleLoginLoading: Boolean = false,
    val isGitHubLoginLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val infoMessage: UiText? = null,
    val navigateToRecordScreen: Boolean = false,
    val navigateToForgotPassword: Boolean = false,
    val navigateToEmailVerification: Boolean = false,
    val isFromOnboarding: Boolean = false,
    val isFromLogout: Boolean = false,
    val welcomeType: WelcomeMessageType = WelcomeMessageType.RETURNING
) {
    val isAnyLoading: Boolean
        get() = isEmailLoginLoading || isGoogleLoginLoading || isGitHubLoginLoading
}

sealed class LoginScreenEvent {
    data class ShowInfoMessage(val message: UiText) : LoginScreenEvent()
    object LaunchGoogleSignIn : LoginScreenEvent()
    data class LaunchGitHubSignIn(val url: Uri) : LoginScreenEvent()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
    private val prefs: SharedPreferences,
    private val firebaseAuth: FirebaseAuth,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @ApplicationContext private val applicationContext: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _loginScreenEventFlow = MutableSharedFlow<LoginScreenEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val loginScreenEventFlow = _loginScreenEventFlow.asSharedFlow()

    private val _gitHubLoginCompleteSignal = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val gitHubLoginCompleteSignal = _gitHubLoginCompleteSignal.asSharedFlow()


    companion object {
        const val KEY_GITHUB_AUTH_STATE = "github_auth_state"
        const val KEY_AUTH_TOKEN = "auth_token"
        const val TAG = "LoginViewModel"
        const val KEY_FROM_ONBOARDING = "from_onboarding"
        const val KEY_HAS_EVER_LOGGED_IN = "has_ever_logged_in"

        private const val GITHUB_CLIENT_ID = BuildConfig.GITHUB_CLIENT_ID
        const val GITHUB_REDIRECT_URI_SCHEME = "audioscholar"
        const val GITHUB_REDIRECT_URI_HOST = "github-callback"
        private const val GITHUB_REDIRECT_URI = "audioscholar://github-callback"
        private const val GITHUB_AUTH_URL = "https://github.com/login/oauth/authorize"
        private const val GITHUB_SCOPE = "read:user user:email"
    }

    init {
        val isFirstLoginArg = savedStateHandle.get<Boolean>("isFirstLogin") ?: false
        val isFromLogoutArg = savedStateHandle.get<Boolean>("fromLogout") ?: false
        val determinedWelcomeType: WelcomeMessageType

        if (isFirstLoginArg) {
            determinedWelcomeType = WelcomeMessageType.NEW_AFTER_ONBOARDING
            Log.d(TAG, "ViewModel init: Detected isFirstLogin=true argument. Setting WelcomeType to NEW.")
            prefs.edit().putBoolean(SplashActivity.KEY_ONBOARDING_COMPLETE, true).apply()
            Log.d(TAG, "ViewModel init: Marked onboarding as complete in preferences.")

        } else if (isFromLogoutArg) {
            determinedWelcomeType = WelcomeMessageType.RETURNING
            Log.d(TAG, "ViewModel init: Detected fromLogout=true argument. Setting WelcomeType to RETURNING.")

        } else {
            val onboardingCompleted = prefs.getBoolean(SplashActivity.KEY_ONBOARDING_COMPLETE, false)
            val hasEverLoggedIn = prefs.getBoolean(KEY_HAS_EVER_LOGGED_IN, false)

            if (onboardingCompleted && hasEverLoggedIn) {
                 determinedWelcomeType = WelcomeMessageType.RETURNING
                 Log.d(TAG, "ViewModel init: No args, onboarding complete AND hasEverLoggedIn is true. Setting WelcomeType to RETURNING.")
            } else {
                 determinedWelcomeType = WelcomeMessageType.NEW_AFTER_ONBOARDING
                 Log.d(TAG, "ViewModel init: No args, hasEverLoggedIn is false OR onboarding incomplete. Setting WelcomeType to NEW.")
                 if (!onboardingCompleted) {
                     prefs.edit().putBoolean(SplashActivity.KEY_ONBOARDING_COMPLETE, true).apply()
                 }
            }
        }

        _uiState.update { it.copy(welcomeType = determinedWelcomeType) }
    }

    private fun registerDeviceToken() {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(applicationContext)

        if (resultCode != com.google.android.gms.common.ConnectionResult.SUCCESS) {
            Log.w(TAG, "Google Play Services not available or out of date. FCM token registration skipped. ResultCode: $resultCode")
            return
        }

        applicationScope.launch {
            Log.d(TAG, "[registerDeviceToken] Attempting to get initial FCM token...")
            val token = FcmTokenProvider.getCurrentToken()

            if (token != null) {
                Log.d(TAG, "[registerDeviceToken] Initial token retrieved: $token")
                Log.d(TAG, "[registerDeviceToken] Calling NotificationRepository to register initial FCM token.")
                val result = notificationRepository.registerFcmToken(token)
                when (result) {
                     is Resource.Success -> {
                        Log.i(TAG, "[registerDeviceToken] Initial FCM token registration successful.")
                     }
                     is Resource.Error -> {
                         val errorMessage = result.message ?: "Unknown error"
                         Log.e(TAG, "[registerDeviceToken] Initial FCM token registration failed: $errorMessage")
                     }
                     else -> {}
                }
            } else {
                Log.w(TAG, "[registerDeviceToken] Initial FCM Token was null after successful retrieval task.")
            }
        }
    }

    private fun triggerProfilePrefetch() {
        applicationScope.launch {
            Log.d(TAG, "[Prefetch] Triggering background profile prefetch in ApplicationScope...")
            try {
                authRepository.getUserProfile().collect { result ->
                    Log.d(TAG, "[Prefetch] Profile fetch result: ${result::class.simpleName}")
                }
                Log.i(TAG, "[Prefetch] Profile prefetch flow collection completed.")
            } catch (e: CancellationException) {
                Log.w(TAG, "[Prefetch] Prefetch coroutine was cancelled (unexpected in AppScope).", e)
            } catch (e: Exception) {
                Log.w(TAG, "[Prefetch] Error during background profile prefetch: ${e.message}", e)
            }
        }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }
    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun onLoginClick() {
        if (_uiState.value.isAnyLoading) return
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password

        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = UiText.StringResource(R.string.login_error_empty_fields)) }
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(errorMessage = UiText.StringResource(R.string.login_error_invalid_email)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isEmailLoginLoading = true, errorMessage = null) }
            try {
                Log.d(TAG, "Attempting Firebase sign-in for: $email")
                val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user

                if (firebaseUser == null) {
                    Log.w(TAG, "Firebase sign-in successful but user object is null.")
                    _uiState.update { it.copy(isEmailLoginLoading = false, errorMessage = UiText.StringResource(R.string.login_error_user_details)) }
                    return@launch
                }
                Log.d(TAG, "Firebase sign-in successful. UID: ${firebaseUser.uid}")

                if (!firebaseUser.isEmailVerified) {
                    Log.i(TAG, "User email not verified. Redirecting to verification screen.")
                    _uiState.update { it.copy(isEmailLoginLoading = false, navigateToEmailVerification = true) }
                    return@launch
                }

                Log.d(TAG, "Fetching Firebase ID token...")
                val idTokenResult = firebaseUser.getIdToken(true).await()
                val firebaseIdToken = idTokenResult.token

                if (firebaseIdToken == null) {
                    Log.w(TAG, "Firebase ID token retrieval failed (token is null).")
                    _uiState.update { it.copy(isEmailLoginLoading = false, errorMessage = UiText.StringResource(R.string.login_error_auth_token)) }
                    firebaseAuth.signOut()
                    return@launch
                }
                Log.d(TAG, "Firebase ID token fetched successfully.")

                val tokenRequest = FirebaseTokenRequest(idToken = firebaseIdToken)
                Log.d(TAG, "Sending Firebase ID token to backend for verification...")
                when (val backendResult = authRepository.verifyFirebaseToken(tokenRequest)) {
                    is Resource.Success -> {
                        val apiJwt = backendResult.data?.token
                        if (apiJwt != null) {
                            Log.i(TAG, "Backend verification successful. API JWT received.")
                            with(prefs.edit()) {
                                putString(KEY_AUTH_TOKEN, apiJwt)
                                putBoolean(SplashActivity.KEY_IS_LOGGED_IN, true)
                                putBoolean(KEY_HAS_EVER_LOGGED_IN, true)
                                commit()
                            }
                            _uiState.update { it.copy(isEmailLoginLoading = false, navigateToRecordScreen = true) }
                            triggerProfilePrefetch()
                            registerDeviceToken()
                        } else {
                            Log.w(TAG, "Backend verification successful but API JWT was null in response.")
                            _uiState.update { it.copy(isEmailLoginLoading = false, errorMessage = UiText.StringResource(R.string.login_error_missing_api_token)) }
                            firebaseAuth.signOut()
                        }
                    }
                    is Resource.Error -> {
                        Log.w(TAG, "Backend verification failed: ${backendResult.message}")
                        _uiState.update { it.copy(isEmailLoginLoading = false, errorMessage = UiText.StringResource(R.string.login_error_server_validation)) }
                        firebaseAuth.signOut()
                    }
                    is Resource.Loading -> {}
                }

            } catch (e: FirebaseAuthException) {
                Log.w(TAG, "Firebase sign-in failed: ${e.errorCode} - ${e.message}")
                val message = when (e.errorCode) {
                    "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" -> UiText.StringResource(R.string.login_error_invalid_credentials)
                    "ERROR_USER_DISABLED" -> UiText.StringResource(R.string.login_error_account_disabled)
                    "ERROR_INVALID_EMAIL" -> UiText.StringResource(R.string.login_error_invalid_email)
                    else -> UiText.StringResource(R.string.login_error_generic, e.localizedMessage ?: "An unknown authentication error occurred.")
                }
                _uiState.update { it.copy(isEmailLoginLoading = false, errorMessage = message) }
            } catch (e: Exception) {
                Log.e(TAG, "An unexpected error occurred during login: ${e.message}", e)
                _uiState.update { it.copy(isEmailLoginLoading = false, errorMessage = UiText.StringResource(R.string.login_error_generic, e.localizedMessage ?: "An unexpected error occurred.")) }
            } finally {
                if (_uiState.value.isEmailLoginLoading && !_uiState.value.navigateToRecordScreen) {
                    _uiState.update { it.copy(isEmailLoginLoading = false) }
                }
            }
        }
    }

    fun onForgotPasswordClick() {
        if (_uiState.value.isAnyLoading) return
        _uiState.update { it.copy(navigateToForgotPassword = true) }
    }

    fun onForgotPasswordNavigationHandled() {
        _uiState.update { it.copy(navigateToForgotPassword = false) }
    }

    fun onEmailVerificationNavigationHandled() {
        _uiState.update { it.copy(navigateToEmailVerification = false) }
    }

    fun onGoogleSignInClick() {
        if (_uiState.value.isAnyLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isGoogleLoginLoading = true, errorMessage = null) }
            _loginScreenEventFlow.emit(LoginScreenEvent.LaunchGoogleSignIn)
        }
    }

    fun handleGoogleSignInResult(account: GoogleSignInAccount?) {
        if (account?.idToken != null) {
            Log.i(TAG, "[GoogleSignIn] Google Sign-In successful. Verifying...")
            if (!_uiState.value.isGoogleLoginLoading) {
                Log.w(TAG, "[GoogleSignIn] handleGoogleSignInResult called while not loading. Setting isGoogleLoginLoading=true.")
                _uiState.update { it.copy(isGoogleLoginLoading = true, errorMessage = null) }
            }
            viewModelScope.launch {
                val tokenRequest = FirebaseTokenRequest(idToken = account.idToken!!)
                Log.d(TAG, "[GoogleSignIn] Sending Google ID token to backend for verification...")
                when (val backendResult = authRepository.verifyGoogleToken(tokenRequest)) {
                    is Resource.Success -> {
                        val apiJwt = backendResult.data?.token
                        if (apiJwt != null) {
                            Log.i(TAG, "[GoogleSignIn] Backend Success. JWT received.")

                            val user = firebaseAuth.currentUser
                            if (user != null && !user.isEmailVerified) {
                                Log.i(TAG, "[GoogleSignIn] User email not verified. Redirecting to verification.")
                                _uiState.update { it.copy(isGoogleLoginLoading = false, navigateToEmailVerification = true) }
                                return@launch
                            }

                            try {
                                Log.d(TAG, "[GoogleSignIn] Saving prefs...")
                                with(prefs.edit()) {
                                    putString(KEY_AUTH_TOKEN, apiJwt)
                                    putBoolean(SplashActivity.KEY_IS_LOGGED_IN, true)
                                    putBoolean(KEY_HAS_EVER_LOGGED_IN, true)
                                    commit()
                                }
                                Log.d(TAG, "[GoogleSignIn] Prefs saved.")
                                _uiState.update { it.copy(isGoogleLoginLoading = false, navigateToRecordScreen = true) }
                                triggerProfilePrefetch()
                                registerDeviceToken()
                                Log.d(TAG, "[GoogleSignIn] State updated for navigation.")
                            } catch (e: Exception) {
                                Log.e(TAG, "[GoogleSignIn] CRITICAL Exception during pref saving!", e)
                                _uiState.update { it.copy(isGoogleLoginLoading = false, errorMessage = UiText.StringResource(R.string.login_error_critical_verification)) }
                            }
                        } else {
                            Log.w(TAG, "[GoogleSignIn] Backend Success but JWT null.")
                            _uiState.update { it.copy(isGoogleLoginLoading = false, errorMessage = UiText.StringResource(R.string.login_error_missing_api_token)) }
                        }
                    }
                    is Resource.Error -> {
                        Log.w(TAG, "[GoogleSignIn] Backend Error: ${backendResult.message}")
                        _uiState.update { it.copy(isGoogleLoginLoading = false, errorMessage = UiText.StringResource(R.string.login_error_server_validation)) }
                    }
                    is Resource.Loading -> {}
                }
            }
        } else {
            Log.w(TAG, "[GoogleSignIn] Google Sign-In failed or token missing.")
            if (_uiState.value.isGoogleLoginLoading) {
                _uiState.update { it.copy(isGoogleLoginLoading = false, errorMessage = UiText.StringResource(R.string.login_error_google_signin)) }
            }
        }
    }

    fun onGitHubSignInClick() {
        if (_uiState.value.isAnyLoading) return
        viewModelScope.launch {
            val state = UUID.randomUUID().toString()
            with(prefs.edit()) {
                putString(KEY_GITHUB_AUTH_STATE, state)
                commit()
            }
            Log.d(TAG, "Generated and saved GitHub state to SharedPreferences: $state")

            val authUri = Uri.parse(GITHUB_AUTH_URL).buildUpon()
                .appendQueryParameter("client_id", GITHUB_CLIENT_ID)
                .appendQueryParameter("redirect_uri", GITHUB_REDIRECT_URI)
                .appendQueryParameter("scope", GITHUB_SCOPE)
                .appendQueryParameter("state", state)
                .build()

            Log.d(TAG, "Constructed GitHub Auth URL: $authUri")
            _uiState.update { it.copy(isGitHubLoginLoading = true, errorMessage = null) }
            _loginScreenEventFlow.emit(LoginScreenEvent.LaunchGitHubSignIn(authUri))
        }
    }

    fun handleGitHubRedirect(code: String?, state: String?) {
        Log.d(TAG, "[GitHubRedirect] Handling redirect. Code: ${code?.take(10)}..., State: $state")

        if (!_uiState.value.isGitHubLoginLoading) {
            Log.w(TAG, "[GitHubRedirect] handleGitHubRedirect called while not loading. Setting isGitHubLoginLoading=true.")
            _uiState.update { it.copy(isGitHubLoginLoading = true, errorMessage = null) }
        }

        if (code.isNullOrBlank()) {
            Log.w(TAG, "[GitHubRedirect] GitHub redirect failed: Code is null or blank.")
            _uiState.update { it.copy(isGitHubLoginLoading = false, errorMessage = UiText.StringResource(R.string.login_error_github_code_missing)) }
            with(prefs.edit()) { remove(KEY_GITHUB_AUTH_STATE).commit() }
            return
        }

        Log.d(TAG, "[GitHubRedirect] Attempting to retrieve state from SharedPreferences with key: $KEY_GITHUB_AUTH_STATE")
        val expectedState = prefs.getString(KEY_GITHUB_AUTH_STATE, null)
        Log.d(TAG, "[GitHubRedirect] Retrieved state from SharedPreferences: $expectedState")

        with(prefs.edit()) {
            remove(KEY_GITHUB_AUTH_STATE)
            commit()
        }

        if (expectedState == null) {
            Log.e(TAG, "[GitHubRedirect] GitHub redirect failed: Could not retrieve expected state from SharedPreferences.")
            _uiState.update { it.copy(isGitHubLoginLoading = false, errorMessage = UiText.StringResource(R.string.login_error_github_state_missing)) }
            return
        }

        if (state == null || state != expectedState) {
            Log.e(TAG, "[GitHubRedirect] GitHub redirect failed: State mismatch. Expected='$expectedState', Received='$state'")
            _uiState.update { it.copy(isGitHubLoginLoading = false, errorMessage = UiText.StringResource(R.string.login_error_github_state_mismatch)) }
            return
        }
        Log.i(TAG, "[GitHubRedirect] GitHub state verified successfully.")


        viewModelScope.launch {
            val request = GitHubCodeRequest(code = code, state = state)
            Log.d(TAG, "[GitHubRedirect] Sending GitHub code to backend repository...")

            var finalState = _uiState.value.copy(isGitHubLoginLoading = true)
            var signalActivity = false
            var prefetchTriggered = false

            when (val backendResult = authRepository.verifyGitHubCode(request)) {
                is Resource.Success -> {
                    val apiJwt = backendResult.data?.token
                    if (apiJwt != null) {
                        Log.i(TAG, "[GitHubRedirect] Backend Success. JWT received.")

                        val user = firebaseAuth.currentUser
                        if (user != null && !user.isEmailVerified) {
                            Log.i(TAG, "[GitHubRedirect] User email not verified. Redirecting to verification.")
                            _uiState.update { it.copy(isGitHubLoginLoading = false, navigateToEmailVerification = true) }
                            return@launch
                        }

                        try {
                            Log.d(TAG, "[GitHubRedirect] Saving prefs...")
                            with(prefs.edit()) {
                                putString(KEY_AUTH_TOKEN, apiJwt)
                                putBoolean(SplashActivity.KEY_IS_LOGGED_IN, true)
                                putBoolean(KEY_HAS_EVER_LOGGED_IN, true)
                                commit()
                            }
                            Log.d(TAG, "[GitHubRedirect] Prefs saved.")
                            signalActivity = true
                            finalState = finalState.copy(isGitHubLoginLoading = false, errorMessage = null)
                            triggerProfilePrefetch()
                            prefetchTriggered = true
                            registerDeviceToken()
                        } catch (e: Exception) {
                            Log.e(TAG, "[GitHubRedirect] CRITICAL Exception during pref saving!", e)
                            finalState = finalState.copy(isGitHubLoginLoading = false, errorMessage = UiText.StringResource(R.string.login_error_critical_verification))
                        }
                    } else {
                        Log.w(TAG, "[GitHubRedirect] Backend Success but JWT null.")
                        finalState = finalState.copy(isGitHubLoginLoading = false, errorMessage = UiText.StringResource(R.string.login_error_missing_api_token))
                    }
                }
                is Resource.Error -> {
                    Log.w(TAG, "[GitHubRedirect] Backend Error: ${backendResult.message}")
                    finalState = finalState.copy(isGitHubLoginLoading = false, errorMessage = UiText.StringResource(R.string.login_error_server_validation))
                }
                is Resource.Loading -> {
                }
            }

            _uiState.value = finalState
            Log.d(TAG, "[GitHubRedirect] Final UI state updated. isGitHubLoginLoading=${finalState.isGitHubLoginLoading}, Prefetch triggered: $prefetchTriggered")

            if (signalActivity) {
                Log.d(TAG, "[GitHubRedirect] Emitting gitHubLoginCompleteSignal to MainActivity...")
                _gitHubLoginCompleteSignal.emit(Unit)
            }
        }
    }


    fun onNavigationHandled() {
        Log.d(TAG, "onNavigationHandled called, resetting navigateToRecordScreen flag.")
        _uiState.update { it.copy(navigateToRecordScreen = false) }
    }

    fun consumeErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

}