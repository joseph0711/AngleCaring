package com.example.anglecaring.data.model

import com.google.gson.annotations.SerializedName
import java.util.Date

data class SensorReading(
    @SerializedName("sensor_reading_id")
    val sensorReadingId: Int,
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("reading_time")
    val readingTime: Date?,
    @SerializedName("boolean_value")
    val booleanValue: Boolean? = null,
    @SerializedName("numeric_value")
    val numericValue: Float? = null,
    @SerializedName("created_at")
    val createdAt: Date?,
    val isAlarmPoint: Boolean = false
)

// CO threshold levels
object COThresholds {
    const val NORMAL_MAX = 9.4f
    const val WARNING_MAX = 12.4f
    const val DANGER_MAX = 15.4f
    // Above 15.5 is severe
}

// CO2 threshold levels
object CO2Thresholds {
    const val NORMAL_MAX = 700f
    const val WARNING_MAX = 1000f
    const val DANGER_MAX = 2500f
    // Above 2500 is severe
}