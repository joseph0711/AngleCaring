package com.example.anglecaring.data.repository

import android.util.Log
import com.example.anglecaring.data.api.RetrofitClient
import com.example.anglecaring.data.local.SessionManager
import com.example.anglecaring.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import com.example.anglecaring.data.repository.AuthRepository.ProfileUpdateResult

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val sessionManager: SessionManager
) : UserRepository {
    
    private val apiService = RetrofitClient.apiService
    private val TAG = "UserRepositoryImpl"
    
    override suspend fun getUsers(): List<User> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getUsers()
            if (response.isSuccessful && response.body()?.success == true) {
                val users = response.body()?.data ?: emptyList()
                Log.d(TAG, "Successfully fetched ${users.size} users from API")
                return@withContext users
            } else {
                Log.e(TAG, "Failed to fetch users: ${response.message()}")
                return@withContext emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching users", e)
            return@withContext emptyList()
        }
    }
    
    override suspend fun getUserById(userId: Int): User? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getUserById(userId)
            if (response.isSuccessful && response.body()?.success == true) {
                val user = response.body()?.data
                Log.d(TAG, "Successfully fetched user by ID: $userId")
                return@withContext user
            } else {
                Log.e(TAG, "Failed to fetch user by ID: ${response.message()}")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user by ID", e)
            return@withContext null
        }
    }
    
    override suspend fun deleteUser(userId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteUser(userId)
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "Successfully deleted user with ID: $userId")
                return@withContext true
            } else {
                Log.e(TAG, "Failed to delete user: ${response.message()}")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user", e)
            return@withContext false
        }
    }
    
    override suspend fun updateUserAdmin(userId: Int, isAdmin: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = com.example.anglecaring.data.api.UpdateAdminRequest(isAdmin)
            val response = apiService.updateUserAdmin(userId, request)
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "Successfully updated user admin status for ID: $userId to isAdmin: $isAdmin")
                return@withContext true
            } else {
                Log.e(TAG, "Failed to update user admin status: ${response.message()}")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user admin status", e)
            return@withContext false
        }
    }
    
    override suspend fun getUserStats(userId: Int): UserStats? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getUserStats(userId)
            if (response.isSuccessful && response.body()?.success == true) {
                val statsData = response.body()?.data
                if (statsData != null) {
                    val stats = UserStats(
                        loginCount = statsData.loginCount,
                        operationCount = statsData.operationCount,
                        onlineHours = statsData.onlineHours
                    )
                    Log.d(TAG, "Successfully fetched user stats for ID: $userId")
                    return@withContext stats
                }
            }
            Log.e(TAG, "Failed to fetch user stats: ${response.message()}")
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user stats", e)
            return@withContext null
        }
    }

    override suspend fun bulkDeleteUsers(userIds: List<Int>): BulkOperationResult = withContext(Dispatchers.IO) {
        try {
            val request = com.example.anglecaring.data.api.BulkDeleteRequest(userIds)
            val response = apiService.bulkDeleteUsers(request)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                val result = BulkOperationResult(
                    success = true,
                    message = response.body()?.message ?: "批量刪除成功",
                    deletedUsers = data?.deletedUsers?.map { user ->
                        BulkOperationUser(
                            userId = user.userId,
                            userName = user.userName,
                            email = user.email
                        )
                    },
                    failedDeletions = data?.failedDeletions?.map { failure ->
                        BulkOperationFailure(
                            userId = failure.userId,
                            reason = failure.reason
                        )
                    }
                )
                Log.d(TAG, "Successfully bulk deleted users: ${userIds.size} requested")
                return@withContext result
            } else {
                Log.e(TAG, "Failed to bulk delete users: ${response.message()}")
                return@withContext BulkOperationResult(
                    success = false,
                    message = response.message() ?: "批量刪除失敗"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error bulk deleting users", e)
            return@withContext BulkOperationResult(
                success = false,
                message = e.message ?: "批量刪除時發生錯誤"
            )
        }
    }

    override suspend fun bulkUpdateUserAdmin(userIds: List<Int>, isAdmin: Boolean): BulkOperationResult = withContext(Dispatchers.IO) {
        try {
            val request = com.example.anglecaring.data.api.BulkUpdateAdminRequest(userIds, isAdmin)
            val response = apiService.bulkUpdateUserAdmin(request)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                val result = BulkOperationResult(
                    success = true,
                    message = response.body()?.message ?: "批量更新管理員狀態成功",
                    updatedUsers = data?.updatedUsers?.map { user ->
                        BulkOperationUser(
                            userId = user.userId,
                            userName = user.userName,
                            email = user.email,
                            previousAdminStatus = user.previousAdminStatus,
                            newAdminStatus = user.newAdminStatus
                        )
                    },
                    failedUpdates = data?.failedUpdates?.map { failure ->
                        BulkOperationFailure(
                            userId = failure.userId,
                            reason = failure.reason
                        )
                    }
                )
                Log.d(TAG, "Successfully bulk updated user admin status: ${userIds.size} requested, isAdmin: $isAdmin")
                return@withContext result
            } else {
                Log.e(TAG, "Failed to bulk update user admin status: ${response.message()}")
                return@withContext BulkOperationResult(
                    success = false,
                    message = response.message() ?: "批量更新管理員狀態失敗"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error bulk updating user admin status", e)
            return@withContext BulkOperationResult(
                success = false,
                message = e.message ?: "批量更新管理員狀態時發生錯誤"
            )
        }
    }

    override suspend fun updateUserProfile(userId: Int, userName: String?, email: String?, userImage: ByteArray?): ProfileUpdateResult = withContext(Dispatchers.IO) {
        try {
            val request = com.example.anglecaring.data.api.UpdateUserProfileRequest(userName, email, userImage)
            val response = apiService.updateUserProfile(userId, request)
            if (response.isSuccessful && response.body()?.success == true) {
                val userResponse = response.body()?.data
                if (userResponse != null) {
                    Log.d(TAG, "Successfully updated user profile for ID: $userId")
                    return@withContext ProfileUpdateResult.Success(userResponse)
                } else {
                    Log.e(TAG, "Failed to update user profile: No user data in response")
                    return@withContext ProfileUpdateResult.Error("更新用戶資料失敗：無用戶資料")
                }
            } else {
                Log.e(TAG, "Failed to update user profile: ${response.message()}")
                return@withContext ProfileUpdateResult.Error(response.message() ?: "更新用戶資料失敗")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user profile", e)
            return@withContext ProfileUpdateResult.Error(e.message ?: "更新用戶資料時發生錯誤")
        }
    }

    // Get the currently logged in user from SessionManager
    override fun getCurrentUser(): User? {
        try {
            val user = sessionManager.getUser()
            if (user != null) {
                Log.d(TAG, "Current user: ${user.userName} (ID: ${user.id}), isAdmin: ${user.isAdmin}")
            } else {
                Log.w(TAG, "No user logged in")
            }
            return user
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current user", e)
            return null
        }
    }
    
} 