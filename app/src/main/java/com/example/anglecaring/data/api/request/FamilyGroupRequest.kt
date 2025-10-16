package com.example.anglecaring.data.api.request

import com.google.gson.annotations.SerializedName

data class CreateFamilyGroupRequest(
    @SerializedName("groupName") val groupName: String,
    @SerializedName("description") val description: String? = null
)

data class UpdateFamilyGroupRequest(
    @SerializedName("groupName") val groupName: String,
    @SerializedName("description") val description: String? = null
)

data class AddMemberRequest(
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String = "member"
)
