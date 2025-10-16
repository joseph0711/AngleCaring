package com.example.anglecaring.data.repository

import com.example.anglecaring.data.api.AlarmSensorReadingsData
import com.example.anglecaring.data.api.RetrofitClient
import com.example.anglecaring.data.model.AlarmEvent
import com.example.anglecaring.data.model.SensorReading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class AlarmRepository {
    
    private val apiService = RetrofitClient.apiService
    
    /**
     * 獲取特定警報的詳細資訊和相關感測器讀數
     */
    fun getAlarmWithSensorReadings(
        alarmId: Int,
        readingsCount: Int = 10
    ): Flow<Result<AlarmSensorReadingsData>> = flow {
        try {
            val response = apiService.getAlarmSensorReadings(alarmId, readingsCount)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    emit(Result.success(body.data))
                } else {
                    emit(Result.failure(Exception("獲取警報數據失敗")))
                }
            } else {
                emit(Result.failure(Exception("API 請求失敗: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * 僅獲取警報資訊
     */
    fun getAlarmById(alarmId: Int): Flow<Result<AlarmEvent>> = flow {
        try {
            val response = apiService.getAlarmById(alarmId)
            
            if (response.isSuccessful) {
                val alarm = response.body()
                if (alarm != null) {
                    emit(Result.success(alarm))
                } else {
                    emit(Result.failure(Exception("警報不存在")))
                }
            } else {
                emit(Result.failure(Exception("API 請求失敗: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
} 