package com.example.anglecaring.ui.profile

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.anglecaring.data.local.SessionManager
import com.example.anglecaring.data.model.User
import com.example.anglecaring.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

private const val TAG = "ProfileEditViewModel"

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

    var userName by mutableStateOf("")
        private set
    
    var email by mutableStateOf("")
        private set
    
    var currentPassword by mutableStateOf("")
        private set
    
    var newPassword by mutableStateOf("")
        private set
    
    var confirmPassword by mutableStateOf("")
        private set
    
    var selectedImageUri by mutableStateOf<Uri?>(null)
        private set
    
    private var selectedImageBytes: ByteArray? = null

    init {
        Log.d(TAG, "ProfileEditViewModel initialized")
        
        // Debug: Check current session data
        val currentUser = sessionManager.getUser()
        if (currentUser != null) {
            Log.d(TAG, "DEBUG: Raw user data from session - ID: ${currentUser.id}, Email: '${currentUser.email}', UserName: '${currentUser.userName}'")
            if (currentUser.email?.contains("@example.com") == true) {
                Log.w(TAG, "ALERT: Detected mock email data! This indicates the original API response had null/empty email.")
            }
        }
        
        loadCurrentUserProfile()
    }

    private fun loadCurrentUserProfile() {
        val currentUser = sessionManager.getUser()
        Log.d(TAG, "Loading current user profile...")
        
        if (currentUser != null) {
            Log.d(TAG, "Current user found - ID: ${currentUser.id}, Name: ${currentUser.userName}, Email: ${currentUser.email}")
            
            val loadedUserName = currentUser.userName ?: ""
            val loadedEmail = currentUser.email ?: ""
            
            userName = loadedUserName
            email = loadedEmail
            
            Log.d(TAG, "Setting ViewModel values - userName: $userName, email: $email")
            
            // 檢查是否有空的 email
            if (email.isEmpty()) {
                Log.w(TAG, "Warning: User email is empty or null")
            }
            
            // 檢查用戶頭像數據
            val userImageBytes = currentUser.userImage
            if (userImageBytes != null) {
                Log.d(TAG, "User has image data - size: ${userImageBytes.size} bytes")
            } else {
                Log.d(TAG, "User has no image data")
            }
            
            _uiState.value = _uiState.value.copy(
                currentUser = currentUser,
                isLoading = false
            )
        } else {
            Log.e(TAG, "No current user found in session manager")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "無法載入用戶資料"
            )
        }
    }

    fun updateUserName(newUserName: String) {
        userName = newUserName
        clearError()
    }

    fun updateEmail(newEmail: String) {
        email = newEmail
        clearError()
    }

    fun updateCurrentPassword(password: String) {
        currentPassword = password
        clearError()
    }

    fun updateNewPassword(password: String) {
        newPassword = password
        clearError()
    }

    fun updateConfirmPassword(password: String) {
        confirmPassword = password
        clearError()
    }

    fun selectImage(uri: Uri, imageBytes: ByteArray) {
        selectedImageUri = uri
        selectedImageBytes = imageBytes
        clearError()
    }

    fun removeSelectedImage() {
        selectedImageUri = null
        selectedImageBytes = null
    }

    fun updateProfile() {
        if (!validateProfileInput()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                val result = authRepository.updateProfile(
                    userName = userName.trim(),
                    email = email.trim(),
                    userImage = selectedImageBytes
                )

                when (result) {
                    is AuthRepository.ProfileUpdateResult.Success -> {
                        Log.d(TAG, "Profile updated successfully")
                        
                        // Update session with new user data
                        sessionManager.updateUserInfo(result.user)
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isProfileUpdateSuccess = true,
                            successMessage = "個人資料更新成功",
                            currentUser = result.user
                        )
                        
                        // Clear the selected image after successful update
                        removeSelectedImage()
                    }
                    is AuthRepository.ProfileUpdateResult.Error -> {
                        Log.e(TAG, "Profile update failed: ${result.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during profile update", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "發生未預期錯誤，請稍後再試"
                )
            }
        }
    }

    fun updatePassword() {
        if (!validatePasswordInput()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                val result = authRepository.updatePassword(
                    currentPassword = currentPassword,
                    newPassword = newPassword
                )

                when (result) {
                    is AuthRepository.PasswordUpdateResult.Success -> {
                        Log.d(TAG, "Password updated successfully")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isPasswordUpdateSuccess = true,
                            successMessage = "密碼更新成功"
                        )
                        
                        // Clear password fields after successful update
                        clearPasswordFields()
                    }
                    is AuthRepository.PasswordUpdateResult.Error -> {
                        Log.e(TAG, "Password update failed: ${result.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during password update", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "發生未預期錯誤，請稍後再試"
                )
            }
        }
    }

    private fun validateProfileInput(): Boolean {
        when {
            userName.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "用戶名不能為空")
                return false
            }
            email.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "郵箱不能為空")
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _uiState.value = _uiState.value.copy(error = "請輸入有效的郵箱地址")
                return false
            }
        }
        return true
    }

    private fun validatePasswordInput(): Boolean {
        when {
            currentPassword.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "請輸入當前密碼")
                return false
            }
            newPassword.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "請輸入新密碼")
                return false
            }
            newPassword.length < 6 -> {
                _uiState.value = _uiState.value.copy(error = "新密碼至少需要6個字符")
                return false
            }
            confirmPassword.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "請確認新密碼")
                return false
            }
            newPassword != confirmPassword -> {
                _uiState.value = _uiState.value.copy(error = "新密碼和確認密碼不一致")
                return false
            }
            currentPassword == newPassword -> {
                _uiState.value = _uiState.value.copy(error = "新密碼不能與當前密碼相同")
                return false
            }
        }
        return true
    }

    private fun clearPasswordFields() {
        currentPassword = ""
        newPassword = ""
        confirmPassword = ""
    }

    private fun clearError() {
        if (_uiState.value.error != null) {
            _uiState.value = _uiState.value.copy(error = null)
        }
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            isProfileUpdateSuccess = false,
            isPasswordUpdateSuccess = false
        )
    }

    fun clearAllMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            successMessage = null,
            isProfileUpdateSuccess = false,
            isPasswordUpdateSuccess = false
        )
    }
}

data class ProfileEditUiState(
    val isLoading: Boolean = true,
    val currentUser: User? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val isProfileUpdateSuccess: Boolean = false,
    val isPasswordUpdateSuccess: Boolean = false
)
