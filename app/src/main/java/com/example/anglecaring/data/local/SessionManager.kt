package com.example.anglecaring.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.anglecaring.data.api.RetrofitClient
import com.example.anglecaring.data.model.User
import com.example.anglecaring.data.util.JwtUtils
import com.google.gson.Gson
import java.io.IOException
import java.security.GeneralSecurityException
import kotlin.random.Random

/**
 * Session manager to handle user authentication state and securely store user information
 */
class SessionManager(context: Context) {
    
    companion object {
        private const val TAG = "SessionManager"
        private const val PREF_NAME = "angle_caring_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_DATA = "user_data"
        private const val KEY_AUTH_TOKEN = "auth_token"
    }
    
    private val applicationContext = context.applicationContext
    private var prefs: SharedPreferences

    init {
        // Use encrypted shared preferences for security
        prefs = try {
            val masterKey = MasterKey.Builder(applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            EncryptedSharedPreferences.create(
                applicationContext,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: GeneralSecurityException) {
            Log.e(TAG, "Error initializing encrypted shared preferences", e)
            // Fallback to regular shared preferences if encryption fails
            applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        } catch (e: IOException) {
            Log.e(TAG, "Error initializing encrypted shared preferences", e)
            applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
        
        // Load auth token if it exists and user is logged in
        if (isLoggedIn()) {
            getAuthToken()?.let { token ->
                if (token.isNotEmpty()) {
                    Log.d(TAG, "Restoring auth token from session")
                    RetrofitClient.setAuthToken(token)
                }
            }
        }
    }
    
    /**
     * Save user login session
     */
    fun createLoginSession(user: User, token: String? = null) {
        val editor = prefs.edit()
        
        // Log received data for debugging
        Log.d(TAG, "Creating login session with user: ID=${user.id}, name=${user.userName}, email=${user.email}, isAdmin=${user.isAdmin}, token=${token?.take(10)}...")
        
        // Create a completely safe copy of the user
        var updatedUser: User
        try {
            // Check if we need to update the user ID
            val userId = if (user.id <= 0) {
                // Try to extract user ID from token
                if (!token.isNullOrEmpty()) {
                    val tokenUserId = JwtUtils.extractUserId(token)
                    if (tokenUserId != null && tokenUserId > 0) {
                        Log.d(TAG, "Using user ID from token: $tokenUserId")
                        tokenUserId
                    } else {
                        // Try to parse the JWT payload manually for debugging
                        val parts = token.split(".")
                        if (parts.size == 3) {
                            try {
                                val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
                                Log.d(TAG, "JWT payload: $payload")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error decoding JWT payload", e)
                            }
                        }
                        
                        val backupId = Random.nextInt(1000, 2000)
                        Log.d(TAG, "Using backup user ID: $backupId")
                        backupId
                    }
                } else {
                    val backupId = Random.nextInt(1000, 2000)
                    Log.d(TAG, "Using backup user ID: $backupId")
                    backupId
                }
            } else {
                user.id
            }
            
            // Ensure userName is not null or empty
            val safeUserName = if (user.userName?.isNotEmpty() == true) {
                Log.d(TAG, "Using provided userName from API: ${user.userName}")
                user.userName
            } else if (!token.isNullOrEmpty()) {
                // Try to extract userName from token
                val tokenUserName = JwtUtils.extractUserName(token)
                if (!tokenUserName.isNullOrEmpty()) {
                    Log.d(TAG, "Using user name from token: $tokenUserName")
                    tokenUserName
                } else {
                    // Try to parse the JWT payload manually for debugging
                    val parts = token.split(".")
                    if (parts.size == 3) {
                        try {
                            val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
                            Log.d(TAG, "JWT payload for username lookup: $payload")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error decoding JWT payload for username", e)
                        }
                    }
                    
                    // We shouldn't reach here if database properly returns username
                    Log.w(TAG, "Failed to get userName from API or token, using default name")
                    "使用者$userId"
                }
            } else {
                Log.w(TAG, "No userName or token available, using default name")
                "使用者$userId"
            }
            
            // Check email and log if it's missing
            val safeEmail = if (user.email.isNullOrEmpty()) {
                Log.w(TAG, "Warning: User email is null or empty from API response. User ID: $userId")
                user.email ?: ""  // Keep original email (null becomes empty string)
            } else {
                Log.d(TAG, "User email from API: ${user.email}")
                user.email
            }
            
            // Ensure password is not null
            val safePassword = user.password ?: "password_$userId"
            
            // Debug log for isAdmin value
            Log.d(TAG, "Original isAdmin value from API: ${user.isAdmin} (${user.isAdmin.javaClass.simpleName})")
            
            // Debug log for user image data
            if (user._userImageBase64 != null) {
                Log.d(TAG, "User has base64 image data - length: ${user._userImageBase64.length}")
            } else {
                Log.d(TAG, "User has no base64 image data")
            }
            
            // Create a safe user object
            updatedUser = User(
                id = userId,
                userName = safeUserName,
                email = safeEmail,
                password = safePassword,
                accountCreatedTime = user.accountCreatedTime,
                isAdmin = user.isAdmin,
                _userImageBase64 = user._userImageBase64  // 保存頭像的 base64 數據
            )
            
            Log.d(TAG, "Created safe user object: ID=${updatedUser.id}, Name=${updatedUser.userName}, isAdmin=${updatedUser.isAdmin}")
        } catch (e: Exception) {
            // If anything goes wrong, create a completely new user object
            Log.e(TAG, "Error creating safe user object", e)
            val backupId = Random.nextInt(1000, 2000)
            updatedUser = User(
                id = backupId,
                userName = "$backupId",
                email = "user$backupId@example.com",
                password = "password_$backupId",
                isAdmin = false
            )
        }
        
        // Convert User object to JSON string
        val gson = Gson()
        val userJson = gson.toJson(updatedUser)
        
        // Store session data
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putString(KEY_USER_DATA, userJson)
        
        // Store auth token if provided
        if (!token.isNullOrEmpty()) {
            editor.putString(KEY_AUTH_TOKEN, token)
            Log.d(TAG, "Auth token saved to session")
        }
        
        editor.apply()
        Log.d(TAG, "用戶會話已創建: ID=${updatedUser.id}, 郵箱=${updatedUser.email}")
        
        // Verify the saved data immediately
        val savedUser = getUser()
        Log.d(TAG, "驗證保存的用戶數據: ID=${savedUser?.id}, 名稱=${savedUser?.userName}, isAdmin=${savedUser?.isAdmin}")
    }
    
    /**
     * Update the authentication token
     */
    fun saveAuthToken(token: String) {
        if (token.isEmpty()) return
        
        val editor = prefs.edit()
        editor.putString(KEY_AUTH_TOKEN, token)
        editor.apply()
        Log.d(TAG, "Auth token updated in session, token length: ${token.length}")
    }
    
    /**
     * Get stored authentication token
     */
    fun getAuthToken(): String? {
        val token = prefs.getString(KEY_AUTH_TOKEN, null)
        if (token != null) {
            Log.d(TAG, "Retrieved auth token from session, token length: ${token.length}")
        } else {
            Log.w(TAG, "No auth token found in session")
        }
        return token
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    /**
     * Get stored user data
     */
    fun getUser(): User? {
        val userJson = prefs.getString(KEY_USER_DATA, null) ?: return null
        
        return try {
            val gson = Gson()
            val user = gson.fromJson(userJson, User::class.java)
            
            // 驗證用戶ID
            if (user != null && user.id <= 0) {
                Log.w(TAG, "警告：從會話中獲取的用戶ID無效: ${user.id}")
            }
            
            if (user != null) {
                Log.d(TAG, "從SessionManager獲取用戶: ID=${user.id}, Name=${user.userName}, Email=${user.email}, isAdmin=${user.isAdmin}")
                if (user._userImageBase64 != null) {
                    Log.d(TAG, "從SessionManager獲取的用戶有頭像數據 - base64長度: ${user._userImageBase64.length}")
                } else {
                    Log.d(TAG, "從SessionManager獲取的用戶沒有頭像數據")
                }
            }
            
            user
        } catch (e: Exception) {
            Log.e(TAG, "解析用戶數據時發生錯誤", e)
            null
        }
    }
    
    /**
     * Clear session (logout)
     */
    fun logout() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
        Log.d(TAG, "User logged out")
    }
    
    /**
     * Update user information
     */
    fun updateUserInfo(user: User) {
        if (!isLoggedIn()) {
            Log.w(TAG, "Cannot update user info: No user is logged in")
            return
        }
        
        // 檢查用戶頭像數據
        val userImageBytes = user.userImage
        if (userImageBytes != null) {
            Log.d(TAG, "Updating user with image data - size: ${userImageBytes.size} bytes")
        } else {
            Log.d(TAG, "Updating user with no image data")
        }
        
        val gson = Gson()
        val userJson = gson.toJson(user)
        
        val editor = prefs.edit()
        editor.putString(KEY_USER_DATA, userJson)
        editor.apply()
        
        Log.d(TAG, "User info updated: ${user.email}")
    }
    
    /**
     * Get application context
     */
    fun getApplicationContext(): Context {
        return applicationContext
    }
} 