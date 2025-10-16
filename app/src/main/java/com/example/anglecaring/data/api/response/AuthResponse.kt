package com.example.anglecaring.data.api.response

import com.example.anglecaring.data.model.User

data class AuthResponse(
    val success: Boolean,
    val message: String,
    val user: User? = null,
    val token: String? = null,
    val debug: Map<String, Any>? = null
) 