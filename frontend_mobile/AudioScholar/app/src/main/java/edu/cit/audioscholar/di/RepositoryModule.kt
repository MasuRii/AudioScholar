package edu.cit.audioscholar.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import edu.cit.audioscholar.data.remote.service.ApiService
import edu.cit.audioscholar.data.repository.AdminRepositoryImpl
import edu.cit.audioscholar.domain.repository.AdminRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAdminRepository(apiService: ApiService): AdminRepository {
        return AdminRepositoryImpl(apiService)
    }

}