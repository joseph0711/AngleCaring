package com.example.anglecaring.data.api.response

import com.example.anglecaring.data.model.User

data class ProfileResponse(
    val success: Boolean,
    val message: String,
    val user: User? = null
)

data class PasswordUpdateResponse(
    val success: Boolean,
    val message: String
)
