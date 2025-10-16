package com.example.anglecaring.data.model

import com.google.gson.annotations.SerializedName
import java.util.Date

data class FamilyGroup(
    @SerializedName("groupId") val groupId: Int = 0,
    @SerializedName("groupName") val groupName: String = "",
    @SerializedName("description") val description: String? = null,
    @SerializedName("createdBy") val createdBy: Int = 0,
    @SerializedName("creatorName") val creatorName: String? = null,
    @SerializedName("createdTime") val createdTime: Date? = null,
    @SerializedName("updatedTime") val updatedTime: Date? = null,
    @SerializedName("isActive") val isActive: Boolean = true,
    @SerializedName("role") val role: String? = null, // 當前用戶在此群組中的角色
    @SerializedName("joinedTime") val joinedTime: Date? = null
) {
    fun isAdmin(): Boolean = role == "admin"
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FamilyGroup

        if (groupId != other.groupId) return false
        if (groupName != other.groupName) return false
        if (description != other.description) return false
        if (createdBy != other.createdBy) return false
        if (creatorName != other.creatorName) return false
        if (createdTime != other.createdTime) return false
        if (updatedTime != other.updatedTime) return false
        if (isActive != other.isActive) return false
        if (role != other.role) return false
        if (joinedTime != other.joinedTime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = groupId
        result = 31 * result + groupName.hashCode()
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + createdBy
        result = 31 * result + (creatorName?.hashCode() ?: 0)
        result = 31 * result + (createdTime?.hashCode() ?: 0)
        result = 31 * result + (updatedTime?.hashCode() ?: 0)
        result = 31 * result + isActive.hashCode()
        result = 31 * result + (role?.hashCode() ?: 0)
        result = 31 * result + (joinedTime?.hashCode() ?: 0)
        return result
    }
}
