package com.example.anglecaring.ui.login

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.anglecaring.data.api.RetrofitClient
import com.example.anglecaring.data.local.SessionManager
import com.example.anglecaring.data.model.User
import com.example.anglecaring.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    application: Application,
    private val repository: AuthRepository
) : AndroidViewModel(application) {
    private val sessionManager = SessionManager(application)
    
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState
    
    private val _signupState = MutableStateFlow<SignupState>(SignupState.Idle)
    val signupState: StateFlow<SignupState> = _signupState
    
    // Add companion object with TAG constant
    companion object {
        private const val TAG = "AuthViewModel"
    }
    
    // Initialize state based on saved session
    init {
        // Initialize RetrofitClient with application context
        RetrofitClient.initialize(application)
        checkLoggedInStatus()
    }
    
    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        data class Success(val user: User) : LoginState()
        data class Error(val message: String) : LoginState()
    }
    
    sealed class SignupState {
        object Idle : SignupState()
        object Loading : SignupState()
        data class Success(val user: User) : SignupState()
        data class Error(val message: String) : SignupState()
    }
    
    // Check if user is already logged in
    private fun checkLoggedInStatus() {
        if (sessionManager.isLoggedIn()) {
            val user = sessionManager.getUser()
            if (user != null) {
                Log.d(TAG, "用戶已登入: ID=${user.id}, 名稱=${user.userName}, isAdmin=${user.isAdmin}")
                _loginState.value = LoginState.Success(user)
                
                // Ensure the token is set in RetrofitClient
                sessionManager.getAuthToken()?.let { token ->
                    if (token.isNotEmpty()) {
                        Log.d(TAG, "從會話中恢復令牌")
                        RetrofitClient.setAuthToken(token)
                    } else {
                        Log.w(TAG, "用戶已登入但令牌為空")
                    }
                }
            }
        }
    }
    
    fun login(email: String, password: String) {
        _loginState.value = LoginState.Loading
        
        viewModelScope.launch {
            when (val result = repository.login(email, password)) {
                is AuthRepository.AuthResult.Success -> {
                    // Validate user ID before creating session
                    val user = result.user
                    if (user.id <= 0) {
                        Log.w(TAG, "警告: 登入用戶ID無效: ${user.id}，這可能導致應用程序問題")
                    }
                    
                    // Log complete user details for debugging
                    Log.d(TAG, "準備保存登入會話 - 使用者詳情: ID=${user.id}, 名稱=${user.userName}, Email=${user.email}, 是否管理員=${user.isAdmin}")
                    
                    try {
                        // Save user session with token
                        sessionManager.createLoginSession(user, result.token)
                        Log.d(TAG, "登入成功: ID=${user.id}, 名稱=${user.userName}, isAdmin=${user.isAdmin}")
                        
                        // Double check the saved user has correct admin rights
                        val savedUser = sessionManager.getUser()
                        Log.d(TAG, "從SessionManager檢查保存後的用戶資料 - isAdmin=${savedUser?.isAdmin}")
                        
                        // Store token for future API requests (already done in repository)
                        _loginState.value = LoginState.Success(user)
                    } catch (e: Exception) {
                        Log.e(TAG, "登入時發生錯誤:", e)
                        _loginState.value = LoginState.Error("登入時發生錯誤: ${e.message}")
                    }
                }
                is AuthRepository.AuthResult.Error -> {
                    _loginState.value = LoginState.Error(result.message)
                }
            }
        }
    }
    
    fun signup(userName: String, email: String, password: String, userImage: ByteArray? = null) {
        _signupState.value = SignupState.Loading
        
        viewModelScope.launch {
            when (val result = repository.signup(userName, email, password, userImage)) {
                is AuthRepository.AuthResult.Success -> {
                    // Validate user ID before creating session
                    val user = result.user
                    if (user.id <= 0) {
                        Log.w(TAG, "警告: 註冊用戶ID無效: ${user.id}，這可能導致應用程序問題")
                    }
                    
                    // Log complete user details for debugging
                    Log.d(TAG, "準備保存註冊會話 - 使用者詳情: ID=${user.id}, 名稱=${user.userName}, Email=${user.email}, 是否管理員=${user.isAdmin}")
                    
                    try {
                        // Save user session with token
                        sessionManager.createLoginSession(user, result.token)
                        Log.d(TAG, "註冊成功: ID=${user.id}, 名稱=${user.userName}")
                        
                        // Token is already set in RetrofitClient by the repository
                        _signupState.value = SignupState.Success(user)
                    } catch (e: Exception) {
                        Log.e(TAG, "註冊時發生錯誤:", e)
                        _signupState.value = SignupState.Error("註冊時發生錯誤: ${e.message}")
                    }
                }
                is AuthRepository.AuthResult.Error -> {
                    _signupState.value = SignupState.Error(result.message)
                }
            }
        }
    }
    
    fun logout() {
        sessionManager.logout()
        // Clear the authentication token
        RetrofitClient.setAuthToken("")
        _loginState.value = LoginState.Idle
        _signupState.value = SignupState.Idle
    }
    
    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }
    
    fun resetSignupState() {
        _signupState.value = SignupState.Idle
    }
} 