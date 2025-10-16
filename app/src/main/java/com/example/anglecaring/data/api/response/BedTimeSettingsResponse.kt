package com.example.anglecaring.data.api.response

import com.example.anglecaring.data.model.BedTimeSettings

data class BedTimeSettingsResponse(
    val success: Boolean,
    val data: BedTimeSettings? = null,
    val message: String? = null
)