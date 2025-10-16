package com.example.anglecaring.data.repository

import com.example.anglecaring.data.model.SensorReading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.Date

class SensorRepository {
    
    // Get CO sensor readings for a specific device within a time range
    fun getCOReadingsForDevice(
        deviceId: String,
        startTime: Date,
        endTime: Date
    ): Flow<List<SensorReading>> = flow {
        try {
            // TODO: Implement actual API call to fetch readings from the database
            // For now, we'll emit mock data for demonstration
            val mockReadings = generateMockCOReadings(deviceId, startTime, endTime)
            emit(mockReadings)
        } catch (e: Exception) {
            // In case of error, emit empty list
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)
    
    // Mock function to generate sample CO readings
    private fun generateMockCOReadings(deviceId: String, startTime: Date, endTime: Date): List<SensorReading> {
        val readings = mutableListOf<SensorReading>()
        val startTimeMs = startTime.time
        val endTimeMs = endTime.time
        
        // Generate readings at 10-minute intervals
        var currentTimeMs = startTimeMs
        var id = 1
        
        while (currentTimeMs <= endTimeMs) {
            val readingTime = Date(currentTimeMs)
            
            // Generate a value that gradually increases to simulate a CO leak
            val progress = (currentTimeMs - startTimeMs).toFloat() / (endTimeMs - startTimeMs)
            val value = if (progress < 0.7f) {
                // Normal levels for the first 70% of the time
                (3.0f + (progress * 8.0f)).coerceAtMost(9.0f)
            } else if (progress < 0.8f) {
                // Warning levels
                9.5f + ((progress - 0.7f) * 10.0f * 2.9f)
            } else if (progress < 0.9f) {
                // Danger levels
                12.5f + ((progress - 0.8f) * 10.0f * 2.9f)
            } else {
                // Severe levels
                15.5f + ((progress - 0.9f) * 10.0f * 4.5f)
            }
            
            readings.add(
                SensorReading(
                    sensorReadingId = id++,
                    deviceId = deviceId,
                    readingTime = readingTime,
                    booleanValue = null,
                    numericValue = value,
                    createdAt = readingTime,
                    isAlarmPoint = false
                )
            )
            
            // Move to next interval (10 minutes)
            currentTimeMs += 10 * 60 * 1000
        }
        
        return readings
    }
} 