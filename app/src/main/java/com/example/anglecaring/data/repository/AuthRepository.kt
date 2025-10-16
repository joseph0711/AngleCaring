package com.example.anglecaring.data.repository

import android.util.Log
import com.example.anglecaring.data.api.RetrofitClient
import com.example.anglecaring.data.api.request.LoginRequest
import com.example.anglecaring.data.api.request.SignupRequest
import com.example.anglecaring.data.api.request.UpdateProfileRequest
import com.example.anglecaring.data.api.request.UpdatePasswordRequest
import com.example.anglecaring.data.api.response.ErrorResponse
import com.example.anglecaring.data.model.User
import com.example.anglecaring.data.util.JwtUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AuthRepository"

@Singleton
class AuthRepository @Inject constructor() {
    private val apiService = RetrofitClient.apiService
    
    sealed class AuthResult {
        data class Success(val user: User, val token: String? = null) : AuthResult()
        data class Error(val message: String) : AuthResult()
    }
    
    sealed class ProfileUpdateResult {
        data class Success(val user: User) : ProfileUpdateResult()
        data class Error(val message: String) : ProfileUpdateResult()
    }
    
    sealed class PasswordUpdateResult {
        object Success : PasswordUpdateResult()
        data class Error(val message: String) : PasswordUpdateResult()
    }
    
    suspend fun login(email: String, password: String): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                // Validate inputs
                if (email.isBlank() || password.isBlank()) {
                    return@withContext AuthResult.Error("電子信箱和密碼不能為空")
                }
                
                // Basic email validation
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    return@withContext AuthResult.Error("請輸入有效的電子信箱地址")
                }
                                
                
                val loginRequest = LoginRequest(email, password)
                Log.d(TAG, "Sending login request: $loginRequest")
                
                val response = apiService.login(loginRequest)
                
                Log.d(TAG, "Login response code: ${response.code()}")
                if (response.errorBody() != null) {
                    Log.d(TAG, "Error body: ${response.errorBody()?.string()}")
                }
                
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    Log.d(TAG, "Login response received: success=${authResponse.success}, message=${authResponse.message}")
                    
                    if (authResponse.success && authResponse.user != null) {
                        val user = authResponse.user
                        Log.d(TAG, "Login successful - User details: ID=${user.id}, Email=${user.email}, UserName=${user.userName ?: "N/A"}, IsAdmin=${user.isAdmin}")
                        
                        // Debug: Check user image data
                        val userImageBytes = user.userImage
                        if (userImageBytes != null) {
                            Log.d(TAG, "User has image data - size: ${userImageBytes.size} bytes")
                            val preview = userImageBytes.take(10).joinToString(" ") { "%02x".format(it) }
                            Log.d(TAG, "Image data preview (first 10 bytes): $preview")
                        } else {
                            Log.d(TAG, "User has no image data")
                        }
                        
                        // Debug: Check if email is null or empty
                        if (user.email.isNullOrEmpty()) {
                            Log.w(TAG, "WARNING: User email from API response is null or empty!")
                            Log.d(TAG, "Using login email as fallback: $email")
                        }
                        
                        // Create user with fallback email from login input
                        val userWithEmail = if (user.email.isNullOrEmpty()) {
                            user.copy(email = email) // Use the email from login request
                        } else {
                            user
                        }
                        
                        // Get token directly from response
                        val token = authResponse.token ?: ""
                        
                        // Store token for future requests
                        if (token.isNotEmpty()) {
                            Log.d(TAG, "Setting auth token after login, token length: ${token.length}, first 10 chars: ${token.take(10)}...")
                            RetrofitClient.setAuthToken(token)
                            
                            // Check if the user from response has a valid ID, if not, try to extract from token
                            var user = userWithEmail
                            
                            // Log user details for debugging
                            Log.d(TAG, "User details from response - ID: ${user.id}, Name: ${user.userName ?: "N/A"}, Email: ${user.email}, Admin: ${user.isAdmin}")
                            if (user.id <= 0) {
                                Log.w(TAG, "User ID from response is invalid: ${user.id}, trying to extract from token...")
                                val userId = JwtUtils.extractUserId(token)
                                if (userId != null && userId > 0) {
                                    // Create a new user object with the ID from the token
                                    user = user.copy(id = userId)
                                    Log.d(TAG, "Updated user ID from token: $userId")
                                } else {
                                    Log.e(TAG, "JWT token doesn't contain an 'id' or 'sub' field: ${JwtUtils.decodeToken(token)}")
                                    Log.w(TAG, "Could not extract user ID from token, using default user object")
                                }
                            }
                            
                            return@withContext AuthResult.Success(user, token)
                        } else {
                            Log.w(TAG, "No token received from login response")
                            return@withContext AuthResult.Success(userWithEmail)
                        }
                    } else {
                        Log.w(TAG, "Login failed: ${authResponse.message}")
                        return@withContext AuthResult.Error(authResponse.message)
                    }
                } else {
                    // 嘗試解析錯誤響應中的中文訊息
                    val errorMessage = try {
                        response.errorBody()?.string()?.let { errorBody ->
                            val errorResponse = com.google.gson.Gson().fromJson(errorBody, ErrorResponse::class.java)
                            errorResponse.message
                        }
                    } catch (e: Exception) {
                        null
                    } ?: when (response.code()) {
                        400 -> "登入資料有誤"
                        401 -> "帳號或密碼錯誤"
                        403 -> "帳號被停用"
                        404 -> "使用者不存在"
                        500 -> "伺服器內部錯誤"
                        else -> "登入失敗，請稍後再試"
                    }
                    
                    Log.e(TAG, "Login API error: $errorMessage")
                    return@withContext AuthResult.Error(errorMessage)
                }
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Connection timeout", e)
                return@withContext AuthResult.Error("連線逾時，請檢查網路連線")
            } catch (e: UnknownHostException) {
                Log.e(TAG, "Unable to connect to server", e)
                return@withContext AuthResult.Error("無法連接伺服器，請檢查網路連線")
            } catch (e: HttpException) {
                Log.e(TAG, "HTTP error ${e.code()}", e)
                return@withContext AuthResult.Error("伺服器錯誤，請稍後再試")
            } catch (e: IOException) {
                Log.e(TAG, "Network error", e)
                return@withContext AuthResult.Error("網路錯誤，請檢查網路連線")
            } catch (e: Exception) {
                Log.e(TAG, "Login error", e)
                return@withContext AuthResult.Error("發生未預期錯誤，請稍後再試")
            }
        }
    }
    
    suspend fun signup(userName: String, email: String, password: String, userImage: ByteArray? = null): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                // Validate inputs
                if (userName.isBlank() || email.isBlank() || password.isBlank()) {
                    return@withContext AuthResult.Error("所有欄位都是必需的")
                }
                
                // Basic email validation
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    return@withContext AuthResult.Error("請輸入有效的電子信箱地址")
                }
                
                // Basic password validation
                if (password.length < 6) {
                    return@withContext AuthResult.Error("密碼至少需要6個字符")
                }
                
                // Debug log to verify URL
                Log.d(TAG, "About to call signup API with Retrofit")
                
                val response = apiService.signup(
                    SignupRequest(
                        userName = userName,
                        email = email,
                        password = password,
                        imageBytes = userImage
                    )
                )
                
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    
                    if (authResponse.success && authResponse.user != null) {
                        Log.d(TAG, "Signup successful for user: ${authResponse.user.email}, userName: ${authResponse.user.userName ?: "N/A"}")
                        // Get token directly from response
                        val token = authResponse.token ?: ""
                        
                        // Store token for future requests
                        if (token.isNotEmpty()) {
                            Log.d(TAG, "Setting auth token after signup")
                            RetrofitClient.setAuthToken(token)
                            
                            // Check if the user from response has a valid ID, if not, try to extract from token
                            var user = authResponse.user
                            
                            // Log user details for debugging
                            Log.d(TAG, "User details from signup - ID: ${user.id}, Name: ${user.userName ?: "N/A"}, Email: ${user.email}, Admin: ${user.isAdmin}")
                            if (user.id <= 0) {
                                Log.w(TAG, "User ID from response is invalid: ${user.id}, trying to extract from token...")
                                val userId = JwtUtils.extractUserId(token)
                                if (userId != null && userId > 0) {
                                    // Create a new user object with the ID from the token
                                    user = user.copy(id = userId)
                                    Log.d(TAG, "Updated user ID from token: $userId")
                                } else {
                                    Log.e(TAG, "JWT token doesn't contain an 'id' or 'sub' field: ${JwtUtils.decodeToken(token)}")
                                    Log.w(TAG, "Could not extract user ID from token, using default user object")
                                }
                            }
                            
                            return@withContext AuthResult.Success(user, token)
                        } else {
                            Log.w(TAG, "No token received from signup response")
                            return@withContext AuthResult.Success(authResponse.user)
                        }
                    } else {
                        Log.w(TAG, "Signup failed: ${authResponse.message}")
                        return@withContext AuthResult.Error(authResponse.message)
                    }
                } else {
                    // 嘗試解析錯誤響應中的中文訊息
                    val errorMessage = try {
                        response.errorBody()?.string()?.let { errorBody ->
                            val errorResponse = com.google.gson.Gson().fromJson(errorBody, ErrorResponse::class.java)
                            errorResponse.message
                        }
                    } catch (e: Exception) {
                        null
                    } ?: when (response.code()) {
                        400 -> "註冊資料有誤"
                        409 -> "電子郵件已存在"
                        422 -> "資料格式錯誤"
                        500 -> "伺服器內部錯誤"
                        else -> "註冊失敗，請稍後再試"
                    }
                    
                    Log.e(TAG, "Signup API error: $errorMessage")
                    return@withContext AuthResult.Error(errorMessage)
                }
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Connection timeout", e)
                return@withContext AuthResult.Error("連線逾時，請檢查網路連線")
            } catch (e: UnknownHostException) {
                Log.e(TAG, "Unable to connect to server", e)
                return@withContext AuthResult.Error("無法連接伺服器，請檢查網路連線")
            } catch (e: HttpException) {
                Log.e(TAG, "HTTP error ${e.code()}", e)
                return@withContext AuthResult.Error("伺服器錯誤，請稍後再試")
            } catch (e: IOException) {
                Log.e(TAG, "Network error", e)
                return@withContext AuthResult.Error("網路錯誤，請檢查網路連線")
            } catch (e: Exception) {
                Log.e(TAG, "Signup error", e)
                return@withContext AuthResult.Error("發生未預期錯誤，請稍後再試")
            }
        }
    }
    
    suspend fun updateProfile(userName: String, email: String, userImage: ByteArray? = null): ProfileUpdateResult {
        return withContext(Dispatchers.IO) {
            try {
                // Validate inputs
                if (userName.isBlank() || email.isBlank()) {
                    return@withContext ProfileUpdateResult.Error("用戶名和郵箱不能為空")
                }
                
                // Basic email validation
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    return@withContext ProfileUpdateResult.Error("請輸入有效的郵箱地址")
                }
                
                val updateRequest = UpdateProfileRequest(userName, email, userImage)
                Log.d(TAG, "Sending profile update request for user: $userName")
                
                val response = apiService.updateProfile(updateRequest)
                
                if (response.isSuccessful && response.body() != null) {
                    val profileResponse = response.body()!!
                    
                    if (profileResponse.success && profileResponse.user != null) {
                        Log.d(TAG, "Profile update successful for user: ${profileResponse.user.email}")
                        return@withContext ProfileUpdateResult.Success(profileResponse.user)
                    } else {
                        Log.w(TAG, "Profile update failed: ${profileResponse.message}")
                        return@withContext ProfileUpdateResult.Error(profileResponse.message)
                    }
                } else {
                    val errorMessage = try {
                        response.errorBody()?.string()?.let { errorBody ->
                            val errorResponse = com.google.gson.Gson().fromJson(errorBody, ErrorResponse::class.java)
                            errorResponse.message
                        }
                    } catch (e: Exception) {
                        null
                    } ?: when (response.code()) {
                        400 -> "更新資料格式錯誤"
                        401 -> "未授權，請重新登入"
                        403 -> "沒有權限執行此操作"
                        409 -> "郵箱已被其他用戶使用"
                        500 -> "伺服器內部錯誤"
                        else -> "更新失敗，請稍後再試"
                    }
                    
                    Log.e(TAG, "Profile update API error: $errorMessage")
                    return@withContext ProfileUpdateResult.Error(errorMessage)
                }
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Connection timeout", e)
                return@withContext ProfileUpdateResult.Error("連線逾時，請檢查網路連線")
            } catch (e: UnknownHostException) {
                Log.e(TAG, "Unable to connect to server", e)
                return@withContext ProfileUpdateResult.Error("無法連接伺服器，請檢查網路連線")
            } catch (e: HttpException) {
                Log.e(TAG, "HTTP error ${e.code()}", e)
                return@withContext ProfileUpdateResult.Error("伺服器錯誤，請稍後再試")
            } catch (e: IOException) {
                Log.e(TAG, "Network error", e)
                return@withContext ProfileUpdateResult.Error("網路錯誤，請檢查網路連線")
            } catch (e: Exception) {
                Log.e(TAG, "Profile update error", e)
                return@withContext ProfileUpdateResult.Error("發生未預期錯誤，請稍後再試")
            }
        }
    }
    
    suspend fun updatePassword(currentPassword: String, newPassword: String): PasswordUpdateResult {
        return withContext(Dispatchers.IO) {
            try {
                // Validate inputs
                if (currentPassword.isBlank() || newPassword.isBlank()) {
                    return@withContext PasswordUpdateResult.Error("密碼不能為空")
                }
                
                // Basic password validation
                if (newPassword.length < 6) {
                    return@withContext PasswordUpdateResult.Error("新密碼至少需要6個字符")
                }
                
                val updateRequest = UpdatePasswordRequest(currentPassword, newPassword)
                Log.d(TAG, "Sending password update request")
                
                val response = apiService.updatePassword(updateRequest)
                
                if (response.isSuccessful && response.body() != null) {
                    val passwordResponse = response.body()!!
                    
                    if (passwordResponse.success) {
                        Log.d(TAG, "Password update successful")
                        return@withContext PasswordUpdateResult.Success
                    } else {
                        Log.w(TAG, "Password update failed: ${passwordResponse.message}")
                        return@withContext PasswordUpdateResult.Error(passwordResponse.message)
                    }
                } else {
                    val errorMessage = try {
                        response.errorBody()?.string()?.let { errorBody ->
                            val errorResponse = com.google.gson.Gson().fromJson(errorBody, ErrorResponse::class.java)
                            errorResponse.message
                        }
                    } catch (e: Exception) {
                        null
                    } ?: when (response.code()) {
                        400 -> "密碼格式錯誤"
                        401 -> "當前密碼錯誤"
                        403 -> "沒有權限執行此操作"
                        500 -> "伺服器內部錯誤"
                        else -> "密碼更新失敗，請稍後再試"
                    }
                    
                    Log.e(TAG, "Password update API error: $errorMessage")
                    return@withContext PasswordUpdateResult.Error(errorMessage)
                }
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Connection timeout", e)
                return@withContext PasswordUpdateResult.Error("連線逾時，請檢查網路連線")
            } catch (e: UnknownHostException) {
                Log.e(TAG, "Unable to connect to server", e)
                return@withContext PasswordUpdateResult.Error("無法連接伺服器，請檢查網路連線")
            } catch (e: HttpException) {
                Log.e(TAG, "HTTP error ${e.code()}", e)
                return@withContext PasswordUpdateResult.Error("伺服器錯誤，請稍後再試")
            } catch (e: IOException) {
                Log.e(TAG, "Network error", e)
                return@withContext PasswordUpdateResult.Error("網路錯誤，請檢查網路連線")
            } catch (e: Exception) {
                Log.e(TAG, "Password update error", e)
                return@withContext PasswordUpdateResult.Error("發生未預期錯誤，請稍後再試")
            }
        }
    }
} 