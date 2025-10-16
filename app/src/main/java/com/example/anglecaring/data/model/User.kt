package com.example.anglecaring.data.model

import android.util.Base64
import com.google.gson.annotations.SerializedName
import java.util.Date

data class User(
    @SerializedName("id") val id: Int = -1,
    @SerializedName("userName") val userName: String? = "",
    @SerializedName("email") val email: String? = null,
    @SerializedName("password") val password: String? = "",
    @SerializedName("accountCreatedTime") val accountCreatedTime: Date? = null,
    @SerializedName("isAdmin") val isAdmin: Boolean = false,
    @SerializedName("userImage")
    val _userImageBase64: String? = null
) {
    val userImage: ByteArray?
        get() = try {
            if (_userImageBase64 != null && _userImageBase64.isNotEmpty()) {
                android.util.Log.d("User", "Decoding user image from base64 - length: ${_userImageBase64.length}")
                val decoded = Base64.decode(_userImageBase64, Base64.DEFAULT)
                android.util.Log.d("User", "Successfully decoded user image - size: ${decoded.size} bytes")
                decoded
            } else {
                android.util.Log.d("User", "No base64 image data available")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("User", "Error decoding user image: ${e.message}")
            null
        }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (id != other.id) return false
        if (userName != other.userName) return false
        if (email != other.email) return false
        if (password != other.password) return false
        if (accountCreatedTime != other.accountCreatedTime) return false
        if (isAdmin != other.isAdmin) return false
        if (_userImageBase64 != other._userImageBase64) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + (userName?.hashCode() ?: 0)
        result = 31 * result + (email?.hashCode() ?: 0)
        result = 31 * result + (password?.hashCode() ?: 0)
        result = 31 * result + (accountCreatedTime?.hashCode() ?: 0)
        result = 31 * result + isAdmin.hashCode()
        result = 31 * result + (_userImageBase64?.hashCode() ?: 0)
        return result
    }
} 