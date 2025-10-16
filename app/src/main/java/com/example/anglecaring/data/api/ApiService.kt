package com.example.anglecaring.data.api

import com.example.anglecaring.data.api.request.LoginRequest
import com.example.anglecaring.data.api.request.SignupRequest
import com.example.anglecaring.data.api.request.CreateFamilyGroupRequest
import com.example.anglecaring.data.api.request.UpdateFamilyGroupRequest
import com.example.anglecaring.data.api.request.AddMemberRequest
import com.example.anglecaring.data.api.request.UpdateProfileRequest
import com.example.anglecaring.data.api.request.UpdatePasswordRequest
import com.example.anglecaring.data.api.request.BedTimeSettingsRequest
import com.example.anglecaring.data.api.response.AuthResponse
import com.example.anglecaring.data.api.response.FamilyGroupResponse
import com.example.anglecaring.data.api.response.FamilyGroupDetailsResponse
import com.example.anglecaring.data.api.response.FamilyGroupMemberResponse
import com.example.anglecaring.data.api.response.ProfileResponse
import com.example.anglecaring.data.api.response.PasswordUpdateResponse
import com.example.anglecaring.data.api.response.BedTimeSettingsResponse
import com.example.anglecaring.data.model.AlarmEvent
import com.example.anglecaring.data.model.SensorReading
import com.example.anglecaring.data.model.MonitoringStatusSummary
import com.example.anglecaring.data.model.SensorStatusSummary
import com.example.anglecaring.data.model.DeviceStatus
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<AuthResponse>
    
    @POST("api/auth/signup")
    suspend fun signup(@Body signupRequest: SignupRequest): Response<AuthResponse>
    
    // Profile management endpoints
    @PUT("api/users/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<ProfileResponse>
    
    @PUT("api/users/password")
    suspend fun updatePassword(@Body request: UpdatePasswordRequest): Response<PasswordUpdateResponse>

    @GET("api/alarms/{id}")
    suspend fun getAlarmById(@Path("id") alarmId: Int): Response<AlarmEvent>

    @GET("api/alarms/{alarmId}/sensor-readings")
    suspend fun getAlarmSensorReadings(
        @Path("alarmId") alarmId: Int,
        @Query("readingsCount") readingsCount: Int = 10
    ): Response<AlarmSensorReadingsResponse>

    @GET("api/sensors/{id}/readings")
    suspend fun getSensorReadingsBetweenDates(
        @Path("id") sensorId: Int,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String
    ): Response<List<SensorReading>>
    
    // Family Group endpoints
    @POST("api/family-groups")
    suspend fun createFamilyGroup(@Body request: CreateFamilyGroupRequest): Response<FamilyGroupResponse>
    
    @GET("api/family-groups")
    suspend fun getFamilyGroups(): Response<FamilyGroupResponse>
    
    @GET("api/family-groups/{groupId}")
    suspend fun getFamilyGroupDetails(@Path("groupId") groupId: Int): Response<FamilyGroupDetailsResponse>
    
    @PUT("api/family-groups/{groupId}")
    suspend fun updateFamilyGroup(
        @Path("groupId") groupId: Int,
        @Body request: UpdateFamilyGroupRequest
    ): Response<FamilyGroupResponse>
    
    @DELETE("api/family-groups/{groupId}")
    suspend fun deleteFamilyGroup(@Path("groupId") groupId: Int): Response<FamilyGroupResponse>
    
    @POST("api/family-groups/{groupId}/members")
    suspend fun addFamilyGroupMember(
        @Path("groupId") groupId: Int,
        @Body request: AddMemberRequest
    ): Response<FamilyGroupMemberResponse>
    
    @DELETE("api/family-groups/{groupId}/members/{memberId}")
    suspend fun removeFamilyGroupMember(
        @Path("groupId") groupId: Int,
        @Path("memberId") memberId: Int
    ): Response<FamilyGroupResponse>
    
    // User management endpoints
    @GET("api/users")
    suspend fun getUsers(): Response<UsersResponse>
    
    @GET("api/users/{id}")
    suspend fun getUserById(@Path("id") userId: Int): Response<UserResponse>
    
    @DELETE("api/users/{id}")
    suspend fun deleteUser(@Path("id") userId: Int): Response<ApiResponse<Unit>>
    
    @PUT("api/users/{id}/admin")
    suspend fun updateUserAdmin(
        @Path("id") userId: Int,
        @Body request: UpdateAdminRequest
    ): Response<ApiResponse<Unit>>
    
    @GET("api/users/{id}/stats")
    suspend fun getUserStats(@Path("id") userId: Int): Response<UserStatsResponse>
    
    // 批量操作端點
    @POST("api/users/bulk-delete")
    suspend fun bulkDeleteUsers(@Body request: BulkDeleteRequest): Response<ApiResponse<BulkOperationResponse>>
    
    @POST("api/users/bulk-update-admin")
    suspend fun bulkUpdateUserAdmin(@Body request: BulkUpdateAdminRequest): Response<ApiResponse<BulkOperationResponse>>
    
    @PUT("api/users/{id}/profile")
    suspend fun updateUserProfile(
        @Path("id") userId: Int,
        @Body request: UpdateUserProfileRequest
    ): Response<ApiResponse<com.example.anglecaring.data.model.User>>
    
    // 監控狀態相關端點
    @GET("api/monitoring-status/summary/{userId}")
    suspend fun getMonitoringStatusSummary(@Path("userId") userId: Int): Response<MonitoringStatusResponse>
    
    @GET("api/monitoring-status/latest-alarm/{userId}")
    suspend fun getLatestAlarm(@Path("userId") userId: Int): Response<AlarmResponse>
    
    @GET("api/monitoring-status/device-statuses")
    suspend fun getMonitoringDeviceStatuses(): Response<DeviceStatusResponse>
    
    @GET("api/monitoring-status/latest-sensor-readings")
    suspend fun getLatestSensorReadings(): Response<SensorReadingsResponse>
    
    // 感應器狀態相關端點
    @GET("api/sensor-status/summary/{userId}")
    suspend fun getSensorStatusSummary(@Path("userId") userId: Int): Response<SensorStatusResponse>
    
    @GET("api/sensor-status/device-statuses")
    suspend fun getSensorDeviceStatuses(): Response<DeviceStatusResponse>
    
    // 床時間設定相關端點
    @GET("api/bed-time-settings/{userId}")
    suspend fun getBedTimeSettings(@Path("userId") userId: Int): Response<BedTimeSettingsResponse>
    
    @POST("api/bed-time-settings/{userId}")
    suspend fun createBedTimeSettings(
        @Path("userId") userId: Int,
        @Body request: BedTimeSettingsRequest
    ): Response<BedTimeSettingsResponse>
    
    @PUT("api/bed-time-settings/{userId}")
    suspend fun updateBedTimeSettings(
        @Path("userId") userId: Int,
        @Body request: BedTimeSettingsRequest
    ): Response<BedTimeSettingsResponse>
    
}

// 警報感測器讀數響應格式
data class AlarmSensorReadingsResponse(
    val success: Boolean,
    val data: AlarmSensorReadingsData
)

data class AlarmSensorReadingsData(
    val alarm: AlarmEvent,
    val sensorReadings: List<SensorReading>
)

// 通用API響應格式
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null
)

// 用戶管理相關的請求/響應格式
data class UpdateAdminRequest(
    val isAdmin: Boolean
)

data class UsersResponse(
    val success: Boolean,
    val data: List<com.example.anglecaring.data.model.User>
)

data class UserResponse(
    val success: Boolean,
    val data: com.example.anglecaring.data.model.User
)

data class UserStatsResponse(
    val success: Boolean,
    val data: UserStatsData
)

data class UserStatsData(
    val loginCount: Int,
    val operationCount: Int,
    val onlineHours: Double
)

// 批量操作相關的請求/響應格式
data class BulkDeleteRequest(
    val userIds: List<Int>
)

data class BulkUpdateAdminRequest(
    val userIds: List<Int>,
    val isAdmin: Boolean
)

data class BulkOperationResponse(
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

data class UpdateUserProfileRequest(
    val user_name: String? = null,
    val Email: String? = null,
    val user_image: ByteArray? = null
)

// 監控狀態相關響應格式
data class MonitoringStatusResponse(
    val success: Boolean,
    val data: MonitoringStatusSummary
)

data class AlarmResponse(
    val success: Boolean,
    val data: AlarmEvent?
)

data class DeviceStatusResponse(
    val success: Boolean,
    val data: List<DeviceStatus>
)

data class SensorReadingsResponse(
    val success: Boolean,
    val data: List<SensorReading>
)

// 感應器狀態相關響應格式
data class SensorStatusResponse(
    val success: Boolean,
    val data: SensorStatusSummary
) 