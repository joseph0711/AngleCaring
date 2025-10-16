package com.example.anglecaring.data.repository

import com.example.anglecaring.data.api.ApiService
import com.example.anglecaring.data.api.request.BedTimeSettingsRequest
import com.example.anglecaring.data.api.response.BedTimeSettingsResponse
import com.example.anglecaring.data.model.BedTimeSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BedTimeRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getBedTimeSettings(userId: Int): Result<BedTimeSettings?> {
        return try {
            val response = apiService.getBedTimeSettings(userId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to get bed time settings"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createBedTimeSettings(userId: Int, request: BedTimeSettingsRequest): Result<BedTimeSettings> {
        return try {
            val response = apiService.createBedTimeSettings(userId, request)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                if (data != null) {
                    Result.success(data)
                } else {
                    Result.failure(Exception("No data returned"))
                }
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to create bed time settings"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateBedTimeSettings(userId: Int, request: BedTimeSettingsRequest): Result<BedTimeSettings> {
        return try {
            val response = apiService.updateBedTimeSettings(userId, request)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                if (data != null) {
                    Result.success(data)
                } else {
                    Result.failure(Exception("No data returned"))
                }
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to update bed time settings"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
}