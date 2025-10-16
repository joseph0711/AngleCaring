package com.example.anglecaring.data.api.response

import com.example.anglecaring.data.model.FamilyGroup
import com.example.anglecaring.data.model.FamilyGroupMember
import com.google.gson.annotations.SerializedName

data class FamilyGroupResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("group") val group: FamilyGroup? = null,
    @SerializedName("groups") val groups: List<FamilyGroup>? = null
)

data class FamilyGroupDetailsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("group") val group: FamilyGroup? = null,
    @SerializedName("members") val members: List<FamilyGroupMember>? = null,
    @SerializedName("isAdmin") val isAdmin: Boolean = false
)

data class FamilyGroupMemberResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("member") val member: FamilyGroupMember? = null
)
