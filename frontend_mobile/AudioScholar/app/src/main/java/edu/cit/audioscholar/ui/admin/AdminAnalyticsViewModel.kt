package edu.cit.audioscholar.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.cit.audioscholar.data.remote.dto.analytics.ActivityStatsDto
import edu.cit.audioscholar.data.remote.dto.analytics.ContentEngagementDto
import edu.cit.audioscholar.data.remote.dto.analytics.UserDistributionDto
import edu.cit.audioscholar.domain.repository.AdminRepository
import edu.cit.audioscholar.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminAnalyticsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val activityStats: ActivityStatsDto? = null,
    val userDistribution: UserDistributionDto? = null,
    val contentEngagement: List<ContentEngagementDto> = emptyList()
)

@HiltViewModel
class AdminAnalyticsViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminAnalyticsUiState())
    val uiState: StateFlow<AdminAnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadAnalytics()
    }

    fun loadAnalytics() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            launch { fetchActivityStats() }
            launch { fetchUserDistribution() }
            launch { fetchContentEngagement() }
        }
    }

    private suspend fun fetchActivityStats() {
        adminRepository.getActivityStats().collect { result ->
            handleResult(result) { data ->
                _uiState.update { it.copy(activityStats = data) }
            }
        }
    }

    private suspend fun fetchUserDistribution() {
        adminRepository.getUserDistribution().collect { result ->
            handleResult(result) { data ->
                _uiState.update { it.copy(userDistribution = data) }
            }
        }
    }

    private suspend fun fetchContentEngagement() {
        adminRepository.getContentEngagement().collect { result ->
            handleResult(result) { data ->
                _uiState.update { it.copy(contentEngagement = data ?: emptyList()) }
            }
        }
    }

    private fun <T> handleResult(result: Resource<T>, onSuccess: (T?) -> Unit) {
        when (result) {
            is Resource.Success -> {
                onSuccess(result.data)
                checkLoadingState()
            }
            is Resource.Error -> {
                _uiState.update { it.copy(error = result.message, isLoading = false) }
            }
            is Resource.Loading -> {
                // Individual loading states could be handled here if granular control is needed
            }
        }
    }
    
    // Simple check to turn off loading if we have some data or if we just want to rely on the individual flows completing.
    // Since we launch multiple coroutines, a unified loading state management might be more complex, 
    // but for now, if any errors occur we show error. If successes occur we update data.
    // We can just set loading to false after a delay or let the UI handle partial updates.
    // For simplicity, we'll set loading to false when any Success/Error occurs, 
    // but a better approach would be to wait for all.
    // Given the simple requirement, we'll just ensure loading is false when data arrives.
    private fun checkLoadingState() {
         _uiState.update { it.copy(isLoading = false) }
    }
}