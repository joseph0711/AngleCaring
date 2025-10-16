package com.example.anglecaring.data.api.request

import com.google.gson.annotations.SerializedName

data class BedTimeSettingsRequest(
    @SerializedName("go_to_bed_time") val goToBedTime: String, // Format: "HH:mm:ss"
    @SerializedName("wake_up_time") val wakeUpTime: String, // Format: "HH:mm:ss"
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("alert_if_late") val alertIfLate: Boolean = false,
    @SerializedName("tolerance_minutes") val toleranceMinutes: Int = 15
)