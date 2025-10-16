package com.example.anglecaring.di

import com.example.anglecaring.data.repository.AuthRepository
import com.example.anglecaring.data.repository.BedTimeRepository
import com.example.anglecaring.data.repository.UserRepository
import com.example.anglecaring.data.repository.UserRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(userRepositoryImpl: UserRepositoryImpl): UserRepository
}

@Module
@InstallIn(SingletonComponent::class)
object BedTimeRepositoryModule {
    
    @Provides
    @Singleton
    fun provideBedTimeRepository(apiService: com.example.anglecaring.data.api.ApiService): BedTimeRepository {
        return BedTimeRepository(apiService)
    }
}
