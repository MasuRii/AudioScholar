package edu.cit.audioscholar.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.cit.audioscholar.R
import edu.cit.audioscholar.data.remote.dto.ChangePasswordRequest
import edu.cit.audioscholar.domain.model.PasswordStrength
import edu.cit.audioscholar.domain.repository.AuthRepository
import edu.cit.audioscholar.domain.usecase.PasswordValidator
import edu.cit.audioscholar.util.Resource
import edu.cit.audioscholar.util.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val currentPasswordVisible: Boolean = false,
    val newPasswordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,
    val currentPasswordError: UiText? = null,
    val newPasswordErrors: List<UiText> = emptyList(),
    val confirmPasswordError: UiText? = null,
    val passwordStrength: PasswordStrength = PasswordStrength.NONE,
    val isLoading: Boolean = false,
    val changeSuccess: Boolean = false,
    val generalMessage: UiText? = null
)

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun onCurrentPasswordChange(password: String) {
        _uiState.update {
            it.copy(
                currentPassword = password,
                currentPasswordError = if (it.currentPasswordError != null && password.isNotBlank()) null else it.currentPasswordError,
                generalMessage = if (it.generalMessage != null) null else it.generalMessage
            )
        }
    }

    fun onNewPasswordChange(password: String) {
        val (strength, errors) = PasswordValidator.validatePassword(password)
        val confirmError = if (password != _uiState.value.confirmPassword && _uiState.value.confirmPassword.isNotEmpty()) {
            UiText.StringResource(R.string.settings_password_validation_match)
        } else {
            null
        }
        _uiState.update {
            it.copy(
                newPassword = password,
                newPasswordErrors = errors,
                passwordStrength = strength,
                confirmPasswordError = confirmError ?: it.confirmPasswordError
            )
        }
    }

    fun onConfirmPasswordChange(password: String) {
        val error = if (_uiState.value.newPassword != password && password.isNotEmpty()) {
            UiText.StringResource(R.string.settings_password_validation_match)
        } else {
            null
        }
        _uiState.update {
            it.copy(
                confirmPassword = password,
                confirmPasswordError = error
            )
        }
    }

    fun toggleCurrentPasswordVisibility() {
        _uiState.update { it.copy(currentPasswordVisible = !it.currentPasswordVisible) }
    }

    fun toggleNewPasswordVisibility() {
        _uiState.update { it.copy(newPasswordVisible = !it.newPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(confirmPasswordVisible = !it.confirmPasswordVisible) }
    }

    private fun runFinalValidation(): Boolean {
        val currentPassword = _uiState.value.currentPassword
        val newPassword = _uiState.value.newPassword
        val confirmPassword = _uiState.value.confirmPassword

        val currentError = if (currentPassword.isBlank()) UiText.StringResource(R.string.settings_current_password_required) else null
        val (_, newErrors) = PasswordValidator.validatePassword(newPassword)
        val confirmError = if (newPassword.isNotBlank() && newPassword != confirmPassword) {
            UiText.StringResource(R.string.settings_password_validation_match)
        } else if (newPassword.isNotBlank() && confirmPassword.isBlank()) {
            UiText.StringResource(R.string.settings_confirm_new_password_req)
        } else {
            null
        }

        _uiState.update {
            it.copy(
                currentPasswordError = currentError,
                newPasswordErrors = newErrors,
                confirmPasswordError = confirmError
            )
        }

        if (currentError == null && newErrors.isEmpty() && confirmError == null && currentPassword == newPassword) {
            _uiState.update {
                it.copy(
                    newPasswordErrors = listOf(UiText.StringResource(R.string.settings_new_password_same_as_current))
                )
            }
            return false
        }

        return currentError == null && newErrors.isEmpty() && confirmError == null
    }

    fun changePassword() {
        if (!runFinalValidation()) {
            _uiState.update { it.copy(generalMessage = UiText.StringResource(R.string.settings_password_form_invalid)) }
            return
        }

        val currentUser = firebaseAuth.currentUser
        val email = currentUser?.email
        val currentPassword = _uiState.value.currentPassword
        val newPassword = _uiState.value.newPassword

        if (currentUser == null || email == null) {
            Log.e("ChangePasswordVM", "User not logged in or email unavailable.")
            _uiState.update { it.copy(isLoading = false, generalMessage = UiText.StringResource(R.string.error_auth_required)) }
            return
        }

        val isPasswordProvider = currentUser.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }
        if (!isPasswordProvider) {
            Log.e("ChangePasswordVM", "User does not have a password provider linked.")
            _uiState.update { it.copy(isLoading = false, generalMessage = UiText.StringResource(R.string.error_auth_required)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalMessage = null, currentPasswordError = null) }

            try {
                Log.d("ChangePasswordVM", "Attempting Firebase re-authentication for user: $email")
                val credential = EmailAuthProvider.getCredential(email, currentPassword)
                currentUser.reauthenticate(credential).await()

                Log.i("ChangePasswordVM", "Firebase re-authentication successful. Calling backend API.")
                val backendRequest = ChangePasswordRequest(newPassword = newPassword)
                when (val result = authRepository.changePassword(backendRequest)) {
                    is Resource.Success -> {
                        Log.i("ChangePasswordVM", "Backend password change successful.")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                changeSuccess = true,
                                currentPassword = "",
                                newPassword = "",
                                confirmPassword = ""
                            )
                        }
                    }
                    is Resource.Error -> {
                        Log.e("ChangePasswordVM", "Backend password change failed: ${result.message}")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                generalMessage = result.message?.let { UiText.DynamicString(it) }
                                    ?: UiText.StringResource(R.string.error_password_update_failed)
                            )
                        }
                    }
                    is Resource.Loading -> {
                        Log.d("ChangePasswordVM", "Backend password change in progress...")
                    }
                }

            } catch (e: Exception) {
                Log.e("ChangePasswordVM", "Firebase re-authentication failed.", e)
                val errorMessage = when (e) {
                    is FirebaseAuthInvalidCredentialsException -> UiText.StringResource(R.string.settings_password_validation_current_mock_error)
                    is FirebaseAuthInvalidUserException -> UiText.StringResource(R.string.error_user_not_found_or_disabled)
                    else -> UiText.StringResource(R.string.error_reauth_failed)
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentPasswordError = if (e is FirebaseAuthInvalidCredentialsException) errorMessage else null,
                        generalMessage = if (e !is FirebaseAuthInvalidCredentialsException) errorMessage else null
                    )
                }
            }
        }
    }

    fun consumeGeneralMessage() {
        _uiState.update { it.copy(generalMessage = null) }
    }

    fun resetChangeSuccessFlag() {
        _uiState.update { it.copy(changeSuccess = false) }
    }
}