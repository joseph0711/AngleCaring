package com.example.anglecaring.data.repository

import com.example.anglecaring.data.api.ApiService
import com.example.anglecaring.data.api.request.AddMemberRequest
import com.example.anglecaring.data.api.request.CreateFamilyGroupRequest
import com.example.anglecaring.data.api.request.UpdateFamilyGroupRequest
import com.example.anglecaring.data.api.response.ErrorResponse
import com.example.anglecaring.data.api.response.FamilyGroupDetailsResponse
import com.example.anglecaring.data.api.response.FamilyGroupMemberResponse
import com.example.anglecaring.data.api.response.FamilyGroupResponse
import com.example.anglecaring.data.model.FamilyGroup
import com.example.anglecaring.data.model.FamilyGroupMember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FamilyGroupRepository @Inject constructor(
    private val apiService: ApiService
) {
    
    /**
     * 解析HTTP錯誤響應，獲取中文錯誤訊息
     */
    private fun parseErrorMessage(response: Response<*>): String {
        return try {
            response.errorBody()?.string()?.let { errorBody ->
                val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                errorResponse.message
            }
        } catch (e: Exception) {
            null
        } ?: when (response.code()) {
            400 -> "請求參數錯誤"
            401 -> "未授權，請重新登入"
            403 -> "權限不足"
            404 -> "資源不存在"
            409 -> "資源衝突"
            500 -> "伺服器內部錯誤"
            else -> "網路連線錯誤，請稍後再試"
        }
    }
    
    /**
     * 創建新的家庭群組
     */
    suspend fun createFamilyGroup(
        groupName: String,
        description: String? = null
    ): Result<FamilyGroup> = withContext(Dispatchers.IO) {
        try {
            val request = CreateFamilyGroupRequest(groupName, description)
            val response = apiService.createFamilyGroup(request)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.group != null) {
                    Result.success(body.group)
                } else {
                    Result.failure(Exception(body?.message ?: "創建群組失敗"))
                }
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 獲取用戶的家庭群組列表
     */
    suspend fun getFamilyGroups(): Result<List<FamilyGroup>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getFamilyGroups()
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.groups != null) {
                    Result.success(body.groups)
                } else {
                    Result.failure(Exception(body?.message ?: "獲取群組列表失敗"))
                }
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 獲取群組詳細信息和成員列表
     */
    suspend fun getFamilyGroupDetails(groupId: Int): Result<FamilyGroupDetailsData> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getFamilyGroupDetails(groupId)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.group != null) {
                    Result.success(
                        FamilyGroupDetailsData(
                            group = body.group,
                            members = body.members ?: emptyList(),
                            isAdmin = body.isAdmin
                        )
                    )
                } else {
                    Result.failure(Exception(body?.message ?: "獲取群組詳情失敗"))
                }
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 更新群組信息
     */
    suspend fun updateFamilyGroup(
        groupId: Int,
        groupName: String,
        description: String? = null
    ): Result<FamilyGroup> = withContext(Dispatchers.IO) {
        try {
            val request = UpdateFamilyGroupRequest(groupName, description)
            val response = apiService.updateFamilyGroup(groupId, request)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.group != null) {
                    Result.success(body.group)
                } else {
                    Result.failure(Exception(body?.message ?: "更新群組失敗"))
                }
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 刪除群組
     */
    suspend fun deleteFamilyGroup(groupId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteFamilyGroup(groupId)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(body?.message ?: "刪除群組失敗"))
                }
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 添加成員到群組
     */
    suspend fun addMember(
        groupId: Int,
        email: String,
        role: String = "member"
    ): Result<FamilyGroupMember> = withContext(Dispatchers.IO) {
        try {
            val request = AddMemberRequest(email, role)
            val response = apiService.addFamilyGroupMember(groupId, request)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.member != null) {
                    Result.success(body.member)
                } else {
                    Result.failure(Exception(body?.message ?: "添加成員失敗"))
                }
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 從群組中移除成員
     */
    suspend fun removeMember(groupId: Int, memberId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.removeFamilyGroupMember(groupId, memberId)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(body?.message ?: "移除成員失敗"))
                }
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * 群組詳情數據類
 */
data class FamilyGroupDetailsData(
    val group: FamilyGroup,
    val members: List<FamilyGroupMember>,
    val isAdmin: Boolean
)
