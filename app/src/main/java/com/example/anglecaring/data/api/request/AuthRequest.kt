package com.example.anglecaring.data.api.request

import android.util.Base64

data class LoginRequest(
    val email: String,
    val password: String
) {
    override fun toString(): String {
        return "LoginRequest(email='$email', password='${password.take(2)}****')"
    }
}

data class SignupRequest(
    val userName: String,
    val email: String,
    val password: String,
    val userImage: String? = null  // Base64 encoded string
) {
    constructor(userName: String, email: String, password: String, imageBytes: ByteArray?) : this(
        userName = userName,
        email = email,
        password = password,
        userImage = if (imageBytes != null) Base64.encodeToString(imageBytes, Base64.DEFAULT) else null
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SignupRequest

        if (userName != other.userName) return false
        if (email != other.email) return false
        if (password != other.password) return false
        if (userImage != other.userImage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userName.hashCode()
        result = 31 * result + email.hashCode()
        result = 31 * result + password.hashCode()
        result = 31 * result + (userImage?.hashCode() ?: 0)
        return result
    }
} 