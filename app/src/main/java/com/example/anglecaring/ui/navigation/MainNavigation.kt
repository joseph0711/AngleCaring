package com.example.anglecaring.ui.navigation

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.anglecaring.data.model.User
import com.example.anglecaring.ui.alarmhistory.AlarmHistoryPage
import com.example.anglecaring.ui.home.HomePage
import com.example.anglecaring.ui.login.AuthNavigation
import com.example.anglecaring.ui.login.AuthViewModel
import com.example.anglecaring.ui.sensorhistory.SensorHistoryPage
import com.example.anglecaring.data.local.SessionManager
import androidx.compose.ui.platform.LocalContext
import com.example.anglecaring.ui.alarmhistory.COAlarmDetailPage
import com.example.anglecaring.ui.alarmhistory.CO2AlarmDetailPage
import com.example.anglecaring.ui.familygroup.FamilyGroupListPage
import com.example.anglecaring.ui.familygroup.FamilyGroupDetailsPage
import com.example.anglecaring.ui.profile.ProfileEditPage
import com.example.anglecaring.ui.bedtime.BedtimePage

enum class AppScreen {
    AUTH,
    HOME,
    SENSOR_HISTORY,
    ALARM_HISTORY,
    FAMILY_GROUP_LIST,
    FAMILY_GROUP_DETAILS,
    CO_ALARM_DETAIL,
    CO2_ALARM_DETAIL,
    PROFILE_EDIT,
    MONITORING_STATUS,
    SENSOR_STATUS,
    BEDTIME_SETTINGS,
}

data class UserData(
    val id: Int = 0,
    val name: String? = null,
    val email: String? = null,
    val userImage: ByteArray? = null
) {
    // Return a default name if it's null
    fun getUserName(): String {
        return name ?: "User"
    }
    
    // Return a safe email value
    fun getSafeEmail(): String {
        return email ?: ""
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserData

        if (name != other.name) return false
        if (email != other.email) return false
        if (userImage != null) {
            if (other.userImage == null) return false
            if (!userImage.contentEquals(other.userImage)) return false
        } else if (other.userImage != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + (email?.hashCode() ?: 0)
        result = 31 * result + (userImage?.contentHashCode() ?: 0)
        return result
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainNavigation(
    authViewModel: AuthViewModel = hiltViewModel(),
    onLogout: () -> Unit = {}
) {
    var currentScreen by remember { 
        mutableStateOf(AppScreen.AUTH).also { 
            Log.d("MainNavigation", "初始化 currentScreen: ${AppScreen.AUTH}")
        } 
    }
    
    // 監聽 currentScreen 的變化
    LaunchedEffect(currentScreen) {
        Log.d("MainNavigation", "currentScreen 已變更為: $currentScreen")
    }
    var userData by remember { mutableStateOf(UserData()) }
    var currentAlarm by remember { mutableStateOf<com.example.anglecaring.data.model.AlarmEvent?>(null) }
    var selectedGroupId by remember { mutableStateOf<Int?>(null) }
    
    val loginState by authViewModel.loginState.collectAsState()
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    
    // Handle login state changes
    LaunchedEffect(loginState) {
        Log.d("MainNavigation", "LaunchedEffect 觸發，loginState: ${loginState::class.java.simpleName}")
        when (loginState) {
            is AuthViewModel.LoginState.Success -> {
                val user = (loginState as AuthViewModel.LoginState.Success).user
                userData = UserData(
                    id = user.id,
                    name = user.userName ?: "", // Handle potential null userName
                    email = user.email, // Keep email nullable
                    userImage = user.userImage
                )
                Log.d("MainNavigation", "UserData created with email: ${userData.email ?: "null"}")
                Log.d("MainNavigation", "用戶登入成功：ID=${user.id}, 名稱=${user.userName ?: "Unknown"}")
                
                // 所有用戶都直接導航到 HOME 頁面
                Log.d("MainNavigation", "用戶登入成功，導航到 HOME")
                Log.d("MainNavigation", "currentScreen 變更前: $currentScreen")
                currentScreen = AppScreen.HOME
                Log.d("MainNavigation", "currentScreen 變更後: $currentScreen")
            }
            is AuthViewModel.LoginState.Idle, is AuthViewModel.LoginState.Error, is AuthViewModel.LoginState.Loading -> {
                // Stay on AUTH screen or handle other states as needed
            }
        }
    }
    
    // 初始化時檢查是否已有登入會話並同步用戶資料
    LaunchedEffect(Unit) {
        if (sessionManager.isLoggedIn() && userData.id == 0) {
            val user = sessionManager.getUser()
            if (user != null) {
                userData = UserData(
                    id = user.id,
                    name = user.userName ?: "",
                    email = user.email,
                    userImage = user.userImage
                )
                Log.d("MainNavigation", "初始化時同步用戶資料: 名稱=${user.userName}, ID=${user.id}")
            }
        }
    }
    
    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { 
            val duration = 300
            
            // 使用淡入淡出動畫代替滑動動畫
            fadeIn(animationSpec = tween(duration)) togetherWith 
            fadeOut(animationSpec = tween(duration))
        },
        label = "Screen Transition"
    ) { screen ->
        when (screen) {
            AppScreen.AUTH -> {
                AuthNavigation(
                    authViewModel = authViewModel,
                    onLoginSuccess = { email, _ ->
                        // No handling needed here, as we use the LaunchedEffect above
                    },
                    onSignupSuccess = { name, email, _ ->
                        // No handling needed here, as we use the LaunchedEffect above
                    }
                )
            }
            AppScreen.HOME -> {
                val user = sessionManager.getUser()
                Log.d("MainNavigation", "進入 HOME 畫面 - 用戶: ${user?.userName}")
                
                // 優先使用 loginState 中的用戶資料，如果不可用則使用 sessionManager
                val currentLoginState = loginState
                val currentUser = when (currentLoginState) {
                    is AuthViewModel.LoginState.Success -> currentLoginState.user
                    else -> user
                }
                
                Log.d("MainNavigation", "使用的用戶資料來源: ${if (currentLoginState is AuthViewModel.LoginState.Success) "loginState" else "sessionManager"}")
                Log.d("MainNavigation", "最終用戶資料 - 名稱: ${currentUser?.userName}")
                
                // 確保用戶資料同步到 userData，優先使用 currentUser
                val effectiveUser = currentUser ?: user
                if (effectiveUser != null && userData.id == 0) {
                    userData = UserData(
                        id = effectiveUser.id,
                        name = effectiveUser.userName ?: "",
                        email = effectiveUser.email,
                        userImage = effectiveUser.userImage
                    )
                    Log.d("MainNavigation", "同步用戶資料到 userData: 名稱=${effectiveUser.userName}")
                }
                
                // 優先使用最新的用戶資料
                val displayUserName = effectiveUser?.userName ?: userData.getUserName()
                val displayUserId = effectiveUser?.id?.toString() ?: userData.id.toString()
                val displayUserImage = effectiveUser?.userImage ?: userData.userImage
                
                Log.d("MainNavigation", "HomePage 顯示用戶資料: 名稱=$displayUserName, ID=$displayUserId")
                
                HomePage(
                    userName = displayUserName,
                    userImage = displayUserImage,
                    userId = displayUserId,
                    onLogout = {
                        authViewModel.logout() // Properly log out through ViewModel
                        userData = UserData(id = 0, name = "", email = null, userImage = null) // Clear user data with safe defaults
                        currentScreen = AppScreen.AUTH
                        onLogout()
                    },
                    onNavigateToSensorHistory = {
                        currentScreen = AppScreen.SENSOR_HISTORY
                    },
                    onNavigateToAlarmHistory = {
                        currentScreen = AppScreen.ALARM_HISTORY
                    },
                    onNavigateToFamilyGroup = {
                        currentScreen = AppScreen.FAMILY_GROUP_LIST
                    },
                    onNavigateToProfile = {
                        currentScreen = AppScreen.PROFILE_EDIT
                    },
                    onNavigateToMonitoringStatus = {
                        currentScreen = AppScreen.MONITORING_STATUS
                    },
                    onNavigateToSensorStatus = {
                        currentScreen = AppScreen.SENSOR_STATUS
                    },
                    onNavigateToBedtimeSettings = {
                        currentScreen = AppScreen.BEDTIME_SETTINGS
                    },
                )
            }
            AppScreen.SENSOR_HISTORY -> {
                SensorHistoryPage(
                    onNavigateBack = {
                        currentScreen = AppScreen.HOME
                    }
                )
            }
            AppScreen.ALARM_HISTORY -> {
                AlarmHistoryPage(
                    onNavigateBack = {
                        currentScreen = AppScreen.HOME
                    },
                    onNavigateToAlarmDetail = { alarm ->
                        currentAlarm = alarm
                        // 根據設備類型導航到對應的詳情頁面
                        currentScreen = when (alarm.deviceName?.lowercase()) {
                            "co2" -> AppScreen.CO2_ALARM_DETAIL
                            "co" -> AppScreen.CO_ALARM_DETAIL
                            else -> AppScreen.CO_ALARM_DETAIL // 默認為 CO 詳情頁面
                        }
                    }
                )
            }
            AppScreen.CO_ALARM_DETAIL -> {
                val alarm = currentAlarm
                if (alarm != null) {
                    COAlarmDetailPage(
                        alarm = alarm,
                        onNavigateBack = {
                            currentScreen = AppScreen.ALARM_HISTORY
                            currentAlarm = null
                        }
                    )
                }
            }
            AppScreen.CO2_ALARM_DETAIL -> {
                val alarm = currentAlarm
                if (alarm != null) {
                    CO2AlarmDetailPage(
                        alarm = alarm,
                        onNavigateBack = {
                            currentScreen = AppScreen.ALARM_HISTORY
                            currentAlarm = null
                        }
                    )
                }
            }
            AppScreen.FAMILY_GROUP_LIST -> {
                FamilyGroupListPage(
                    onNavigateToGroupDetails = { groupId ->
                        selectedGroupId = groupId
                        currentScreen = AppScreen.FAMILY_GROUP_DETAILS
                    },
                    onNavigateBack = {
                        currentScreen = AppScreen.HOME
                    }
                )
            }
            AppScreen.FAMILY_GROUP_DETAILS -> {
                val groupId = selectedGroupId
                if (groupId != null) {
                    FamilyGroupDetailsPage(
                        groupId = groupId,
                        onNavigateBack = {
                            currentScreen = AppScreen.FAMILY_GROUP_LIST
                            selectedGroupId = null
                        }
                    )
                }
            }
            AppScreen.PROFILE_EDIT -> {
                ProfileEditPage(
                    onNavigateBack = {
                        // 所有用戶都返回 HOME 頁面
                        currentScreen = AppScreen.HOME
                    }
                )
            }
            AppScreen.MONITORING_STATUS -> {
                com.example.anglecaring.ui.monitoringstatus.MonitoringStatusPage(
                    userId = userData.id,
                    onNavigateBack = {
                        currentScreen = AppScreen.HOME
                    }
                )
            }
            AppScreen.SENSOR_STATUS -> {
                com.example.anglecaring.ui.sensorstatus.SensorStatusPage(
                    userId = userData.id,
                    onNavigateBack = {
                        currentScreen = AppScreen.HOME
                    }
                )
            }
            AppScreen.BEDTIME_SETTINGS -> {
                BedtimePage(
                    userId = userData.id,
                    onNavigateBack = {
                        currentScreen = AppScreen.HOME
                    }
                )
            }
        }
    }
} 