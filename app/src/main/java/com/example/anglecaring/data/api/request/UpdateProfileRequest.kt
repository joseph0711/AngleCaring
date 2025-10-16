package com.example.anglecaring.data.api.request

import com.google.gson.annotations.SerializedName

data class UpdateProfileRequest(
    @SerializedName("user_name")
    val userName: String? = null,
    @SerializedName("Email")
    val email: String? = null,
    @SerializedName("user_image")
    val userImage: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UpdateProfileRequest

        if (userName != other.userName) return false
        if (email != other.email) return false
        if (userImage != null) {
            if (other.userImage == null) return false
            if (!userImage.contentEquals(other.userImage)) return false
        } else if (other.userImage != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userName?.hashCode() ?: 0
        result = 31 * result + (email?.hashCode() ?: 0)
        result = 31 * result + (userImage?.contentHashCode() ?: 0)
        return result
    }
}

data class UpdatePasswordRequest(
    @SerializedName("current_password")
    val currentPassword: String,
    @SerializedName("new_password")
    val newPassword: String
)
