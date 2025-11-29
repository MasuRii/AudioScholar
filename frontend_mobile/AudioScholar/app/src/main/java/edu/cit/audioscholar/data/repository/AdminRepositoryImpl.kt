package edu.cit.audioscholar.data.repository

import edu.cit.audioscholar.data.remote.dto.admin.AdminUpdateUserRolesRequest
import edu.cit.audioscholar.data.remote.dto.admin.AdminUpdateUserStatusRequest
import edu.cit.audioscholar.data.remote.dto.admin.AdminUserListResponse
import edu.cit.audioscholar.data.remote.dto.analytics.ActivityStatsDto
import edu.cit.audioscholar.data.remote.dto.analytics.AnalyticsOverviewDto
import edu.cit.audioscholar.data.remote.dto.analytics.ContentEngagementDto
import edu.cit.audioscholar.data.remote.dto.analytics.UserDistributionDto
import edu.cit.audioscholar.data.remote.service.ApiService
import edu.cit.audioscholar.domain.repository.AdminRepository
import edu.cit.audioscholar.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : AdminRepository {

    override fun getUsers(limit: Int, pageToken: String?): Flow<Resource<AdminUserListResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getUsers(limit, pageToken)
            if (response.isSuccessful) {
                response.body()?.let {
                    emit(Resource.Success(it))
                } ?: emit(Resource.Error("Response body is null"))
            } else {
                emit(Resource.Error("Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Network connection error: ${e.message}"))
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message}"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }

    override fun updateUserStatus(uid: String, disabled: Boolean): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.updateUserStatus(uid, AdminUpdateUserStatusRequest(disabled))
            if (response.isSuccessful) {
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error("Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Network connection error: ${e.message}"))
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message}"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }

    override fun updateUserRoles(uid: String, roles: List<String>): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.updateUserRoles(uid, AdminUpdateUserRolesRequest(roles))
            if (response.isSuccessful) {
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error("Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Network connection error: ${e.message}"))
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message}"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }

    override fun getAnalyticsOverview(): Flow<Resource<AnalyticsOverviewDto>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getAnalyticsOverview()
            if (response.isSuccessful) {
                response.body()?.let {
                    emit(Resource.Success(it))
                } ?: emit(Resource.Error("Response body is null"))
            } else {
                emit(Resource.Error("Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Network connection error: ${e.message}"))
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message}"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }

    override fun getActivityStats(): Flow<Resource<ActivityStatsDto>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getActivityStats()
            if (response.isSuccessful) {
                response.body()?.let {
                    emit(Resource.Success(it))
                } ?: emit(Resource.Error("Response body is null"))
            } else {
                emit(Resource.Error("Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Network connection error: ${e.message}"))
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message}"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }

    override fun getUserDistribution(): Flow<Resource<UserDistributionDto>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getUserDistribution()
            if (response.isSuccessful) {
                response.body()?.let {
                    emit(Resource.Success(it))
                } ?: emit(Resource.Error("Response body is null"))
            } else {
                emit(Resource.Error("Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Network connection error: ${e.message}"))
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message}"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }

    override fun getContentEngagement(): Flow<Resource<List<ContentEngagementDto>>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getContentEngagement()
            if (response.isSuccessful) {
                response.body()?.let {
                    emit(Resource.Success(it))
                } ?: emit(Resource.Error("Response body is null"))
            } else {
                emit(Resource.Error("Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Network connection error: ${e.message}"))
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message}"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }
}