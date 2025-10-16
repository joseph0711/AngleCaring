package com.example.anglecaring.data.model

import java.io.Serializable


// 代表警報事件的數據模型
data class AlarmEvent(
    val alarmId: Int,
    val userId: Int,
    val alarmTime: String,
    val alarmLabel: String?,
    val riskLevel: String,
    val deviceId: String,
    val deviceLocation: String? = null,  // 感應器的位置
    val deviceName: String? = null, // 感應器的名稱
    val value: Float? = null, // 警報值
    val standardValue: Float? = null // 標準值
) : Serializable


// 警報風險級別
object AlarmRiskLevel {
    const val LOW = "警告"
    const val MEDIUM = "危險"
    const val HIGH = "嚴重"
}