package com.example.anglecaring.data.model

import com.google.gson.annotations.SerializedName
import java.sql.Time

data class BedTimeSettings(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("go_to_bed_time") val goToBedTime: String, // Format: "HH:mm:ss"
    @SerializedName("wake_up_time") val wakeUpTime: String, // Format: "HH:mm:ss"
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("alert_if_late") val alertIfLate: Boolean = false,
    @SerializedName("tolerance_minutes") val toleranceMinutes: Int = 15
)