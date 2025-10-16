package com.example.anglecaring.data.model

import android.util.Base64
import com.google.gson.annotations.SerializedName
import java.util.Date

data class FamilyGroupMember(
    @SerializedName("membershipId") val membershipId: Int = 0,
    @SerializedName("groupId") val groupId: Int = 0,
    @SerializedName("userId") val userId: Int = 0,
    @SerializedName("userName") val userName: String = "",
    @SerializedName("email") val email: String = "",
    @SerializedName("role") val role: String = "member", // member, admin
    @SerializedName("joinedTime") val joinedTime: Date? = null,
    @SerializedName("invitedBy") val invitedBy: Int? = null,
    @SerializedName("invitedByName") val invitedByName: String? = null,
    @SerializedName("isActive") val isActive: Boolean = true,
    @SerializedName("userImage")
    private val _userImageBase64: String? = null
) {
    val userImage: ByteArray?
        get() = try {
            if (_userImageBase64 != null && _userImageBase64.isNotEmpty()) {
                Base64.decode(_userImageBase64, Base64.DEFAULT)
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("FamilyGroupMember", "Error decoding user image: ${e.message}")
            null
        }
    fun isAdmin(): Boolean = role == "admin"
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FamilyGroupMember

        if (membershipId != other.membershipId) return false
        if (groupId != other.groupId) return false
        if (userId != other.userId) return false
        if (userName != other.userName) return false
        if (email != other.email) return false
        if (role != other.role) return false
        if (joinedTime != other.joinedTime) return false
        if (invitedBy != other.invitedBy) return false
        if (invitedByName != other.invitedByName) return false
        if (isActive != other.isActive) return false
        if (_userImageBase64 != other._userImageBase64) return false

        return true
    }

    override fun hashCode(): Int {
        var result = membershipId
        result = 31 * result + groupId
        result = 31 * result + userId
        result = 31 * result + userName.hashCode()
        result = 31 * result + email.hashCode()
        result = 31 * result + role.hashCode()
        result = 31 * result + (joinedTime?.hashCode() ?: 0)
        result = 31 * result + (invitedBy ?: 0)
        result = 31 * result + (invitedByName?.hashCode() ?: 0)
        result = 31 * result + isActive.hashCode()
        result = 31 * result + (_userImageBase64?.hashCode() ?: 0)
        return result
    }
}
