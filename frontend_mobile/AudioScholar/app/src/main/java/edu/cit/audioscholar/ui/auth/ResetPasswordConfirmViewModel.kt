package edu.cit.audioscholar.ui.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.cit.audioscholar.R
import edu.cit.audioscholar.domain.model.PasswordStrength
import edu.cit.audioscholar.domain.repository.AuthRepository
import edu.cit.audioscholar.domain.usecase.PasswordValidator
import edu.cit.audioscholar.util.Resource
import edu.cit.audioscholar.util.UiText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResetPasswordConfirmUiState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val newPasswordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,
    val newPasswordErrors: List<UiText> = emptyList(),
    val confirmPasswordError: UiText? = null,
    val passwordStrength: PasswordStrength = PasswordStrength.NONE,
    val isLoading: Boolean = false,
    val generalMessage: UiText? = null,
    val resetSuccess: Boolean = false,
    val oobCode: String = "",
    val countdown: Int = 5
)

sealed class ResetPasswordConfirmEvent {
    object NavigateToLogin : ResetPasswordConfirmEvent()
}

@HiltViewModel
class ResetPasswordConfirmViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResetPasswordConfirmUiState())
    val uiState: StateFlow<ResetPasswordConfirmUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ResetPasswordConfirmEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        val code = savedStateHandle.get<String>("oobCode")
        if (code.isNullOrBlank()) {
            _uiState.update { it.copy(generalMessage = UiText.StringResource(R.string.error_reset_password_invalid_link)) }
        } else {
            _uiState.update { it.copy(oobCode = code) }
        }
    }

    fun onNewPasswordChange(input: String) {
        val (strength, errors) = PasswordValidator.validatePassword(input)
        _uiState.update {
            it.copy(
                newPassword = input,
                passwordStrength = strength,
                newPasswordErrors = errors,
                confirmPasswordError = if (it.confirmPassword.isNotEmpty() && it.confirmPassword != input) UiText.StringResource(R.string.settings_password_validation_match) else null
            )
        }
    }

    fun onConfirmPasswordChange(input: String) {
        _uiState.update {
            it.copy(
                confirmPassword = input,
                confirmPasswordError = if (it.newPassword.isNotEmpty() && it.newPassword != input) UiText.StringResource(R.string.settings_password_validation_match) else null
            )
        }
    }

    fun toggleNewPasswordVisibility() {
        _uiState.update { it.copy(newPasswordVisible = !it.newPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(confirmPasswordVisible = !it.confirmPasswordVisible) }
    }

    fun consumeGeneralMessage() {
        _uiState.update { it.copy(generalMessage = null) }
    }

    fun resetResetSuccessFlag() {
        _uiState.update { it.copy(resetSuccess = false) }
    }

    fun submitPasswordReset() {
        val state = _uiState.value
        if (state.oobCode.isBlank()) {
            _uiState.update { it.copy(generalMessage = UiText.StringResource(R.string.error_reset_password_missing_code)) }
            return
        }
        if (state.newPasswordErrors.isNotEmpty() || state.confirmPasswordError != null || state.newPassword.isBlank()) {
            _uiState.update { it.copy(generalMessage = UiText.StringResource(R.string.error_fix_errors_before_submitting)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authRepository.confirmPasswordReset(state.oobCode, state.newPassword)
            when (result) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, resetSuccess = true) }
                    startCountdown()
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        generalMessage = result.message?.let { UiText.DynamicString(it) } ?: UiText.StringResource(R.string.error_unexpected_default)
                    ) }
                }
                else -> {
                     _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private fun startCountdown() {
        viewModelScope.launch {
            while (_uiState.value.countdown > 0) {
                delay(1000)
                _uiState.update { it.copy(countdown = it.countdown - 1) }
            }
            _eventFlow.emit(ResetPasswordConfirmEvent.NavigateToLogin)
        }
    }

    fun onNavigateToLogin() {
        viewModelScope.launch {
            _eventFlow.emit(ResetPasswordConfirmEvent.NavigateToLogin)
        }
    }
}