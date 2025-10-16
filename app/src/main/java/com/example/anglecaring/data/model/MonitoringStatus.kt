package com.example.anglecaring.data.model

import com.google.gson.annotations.SerializedName
import java.util.Date

// 警報等級枚舉
enum class AlarmLevel(val displayName: String, val color: String) {
    NORMAL("正常", "#4CAF50"),
    WARNING("警告", "#FF9800"),
    SEVERE("嚴重", "#FF5722"),
    DANGER("危險", "#F44336")
}

// 警報數據模型
data class Alarm(
    @SerializedName("alarm_id")
    val alarmId: Int,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("alarm_time")
    val alarmTime: Date,
    @SerializedName("alarm_label")
    val alarmLabel: String?,
    @SerializedName("risk_level")
    val riskLevel: String,
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("created_at")
    val createdAt: Date
)

// 感應器設備狀態
data class DeviceStatus(
    @SerializedName("deviceId")
    val deviceId: String,
    @SerializedName("deviceType")
    val deviceType: String,
    @SerializedName("location")
    val location: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("lastOnlineTime")
    val lastOnlineTime: Date?,
    @SerializedName("isOnline")
    val isOnline: Boolean,
    @SerializedName("lastReadingTime")
    val lastReadingTime: Date?,
    @SerializedName("hasRecentData")
    val hasRecentData: Boolean,
    @SerializedName("isAbnormal")
    val isAbnormal: Boolean
)


// 監控狀態摘要
data class MonitoringStatusSummary(
    val latestAlarm: Alarm?,
    val alarmLevel: AlarmLevel,
    val deviceStatuses: List<DeviceStatus>,
    val latestSensorReadings: List<SensorReading>,
    val overallStatus: String, // "正常" 或 "異常"
    val lastUpdated: Date
)

// 感應器狀態摘要
data class SensorStatusSummary(
    val deviceStatuses: List<DeviceStatus>,
    val normalDevices: List<DeviceStatus>,
    val abnormalDevices: List<DeviceStatus>,
    val overallStatus: String, // "正常" 或 "異常"
    val errorMessages: List<String>,
    val lastUpdated: Date
)
