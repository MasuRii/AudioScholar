package edu.cit.audioscholar.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.cit.audioscholar.data.remote.dto.admin.AdminUserDto
import edu.cit.audioscholar.domain.repository.AdminRepository
import edu.cit.audioscholar.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUserListUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val users: List<AdminUserDto> = emptyList(),
    val nextPageToken: String? = null
)

@HiltViewModel
class AdminUserListViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUserListUiState())
    val uiState: StateFlow<AdminUserListUiState> = _uiState.asStateFlow()

    private val pageSize = 20

    init {
        loadUsers()
    }

    fun loadUsers(reset: Boolean = false) {
        if (reset) {
            _uiState.update { it.copy(users = emptyList(), nextPageToken = null) }
        }

        viewModelScope.launch {
            val currentToken = _uiState.value.nextPageToken
            // If we are not resetting and have no token but already have users, we might be at the end.
            // However, the initial load has null token.
            // Logic: if users is not empty and token is null, we reached end.
            if (!reset && _uiState.value.users.isNotEmpty() && currentToken == null) {
                return@launch
            }

            adminRepository.getUsers(pageSize, if (reset) null else currentToken).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is Resource.Success -> {
                        val newUsers = result.data?.users ?: emptyList()
                        val newToken = result.data?.pageToken
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                users = if (reset) newUsers else state.users + newUsers,
                                nextPageToken = newToken,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                error = result.message ?: "Failed to load users"
                            ) 
                        }
                    }
                }
            }
        }
    }

    fun toggleUserStatus(user: AdminUserDto) {
        viewModelScope.launch {
            val newDisabledStatus = !user.disabled
            adminRepository.updateUserStatus(user.uid, newDisabledStatus).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        // Update local state
                        _uiState.update { state ->
                            val updatedUsers = state.users.map { 
                                if (it.uid == user.uid) it.copy(disabled = newDisabledStatus) else it 
                            }
                            state.copy(users = updatedUsers)
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(error = "Failed to update status: ${result.message}") }
                    }
                    is Resource.Loading -> {
                        // Optional: Show loading indicator for specific user item if needed
                    }
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}