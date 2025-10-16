package com.example.anglecaring.data.repository

import com.example.anglecaring.data.model.User
import com.example.anglecaring.data.repository.AuthRepository.ProfileUpdateResult

interface UserRepository {
    suspend fun getUsers(): List<User>
    suspend fun getUserById(userId: Int): User?
    suspend fun deleteUser(userId: Int): Boolean
    suspend fun updateUserAdmin(userId: Int, isAdmin: Boolean): Boolean
    suspend fun updateUserProfile(userId: Int, userName: String?, email: String?, userImage: ByteArray? = null): ProfileUpdateResult
    suspend fun getUserStats(userId: Int): UserStats?
    suspend fun bulkDeleteUsers(userIds: List<Int>): BulkOperationResult
    suspend fun bulkUpdateUserAdmin(userIds: List<Int>, isAdmin: Boolean): BulkOperationResult
    fun getCurrentUser(): User?
}

data class UserStats(
    val loginCount: Int,
    val operationCount: Int,
    val onlineHours: Double
)

data class BulkOperationResult(
    val success: Boolean,
    val message: String,
    val deletedUsers: List<BulkOperationUser>? = null,
    val updatedUsers: List<BulkOperationUser>? = null,
    val failedDeletions: List<BulkOperationFailure>? = null,
    val failedUpdates: List<BulkOperationFailure>? = null
)

data class BulkOperationUser(
    val userId: Int,
    val userName: String,
    val email: String,
    val previousAdminStatus: Boolean? = null,
    val newAdminStatus: Boolean? = null
)

data class BulkOperationFailure(
    val userId: Int,
    val reason: String
) 