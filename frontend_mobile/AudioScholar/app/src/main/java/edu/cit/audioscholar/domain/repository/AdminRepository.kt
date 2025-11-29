package edu.cit.audioscholar.domain.repository

import edu.cit.audioscholar.data.remote.dto.admin.AdminUserListResponse
import edu.cit.audioscholar.data.remote.dto.analytics.ActivityStatsDto
import edu.cit.audioscholar.data.remote.dto.analytics.AnalyticsOverviewDto
import edu.cit.audioscholar.data.remote.dto.analytics.ContentEngagementDto
import edu.cit.audioscholar.data.remote.dto.analytics.UserDistributionDto
import edu.cit.audioscholar.util.Resource
import kotlinx.coroutines.flow.Flow

interface AdminRepository {
    fun getUsers(limit: Int, pageToken: String?): Flow<Resource<AdminUserListResponse>>
    fun updateUserStatus(uid: String, disabled: Boolean): Flow<Resource<Unit>>
    fun updateUserRoles(uid: String, roles: List<String>): Flow<Resource<Unit>>
    fun getAnalyticsOverview(): Flow<Resource<AnalyticsOverviewDto>>
    fun getActivityStats(): Flow<Resource<ActivityStatsDto>>
    fun getUserDistribution(): Flow<Resource<UserDistributionDto>>
    fun getContentEngagement(): Flow<Resource<List<ContentEngagementDto>>>
}