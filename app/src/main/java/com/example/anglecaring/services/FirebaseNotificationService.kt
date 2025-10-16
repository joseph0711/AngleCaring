package com.example.anglecaring.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.anglecaring.MainActivity
import com.example.anglecaring.R
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirebaseNotificationService {
    
    companion object {
        private const val TAG = "FirebaseNotificationService"
        
        // 警告等級通知頻道 (IMPORTANCE_LOW)
        private const val WARNING_CHANNEL_ID = "angle_caring_warning"
        private const val WARNING_CHANNEL_NAME = "警告通知"
        private const val WARNING_CHANNEL_DESCRIPTION = "低優先級警告通知"
        
        // 危險等級通知頻道 (IMPORTANCE_DEFAULT)
        private const val DANGER_CHANNEL_ID = "angle_caring_danger"
        private const val DANGER_CHANNEL_NAME = "危險通知"
        private const val DANGER_CHANNEL_DESCRIPTION = "中等優先級危險通知"
        
        // 嚴重等級通知頻道 (IMPORTANCE_HIGH) - 緊急通知
        private const val CRITICAL_CHANNEL_ID = "angle_caring_critical"
        private const val CRITICAL_CHANNEL_NAME = "緊急通知"
        private const val CRITICAL_CHANNEL_DESCRIPTION = "最高優先級緊急通知，可繞過勿擾模式"
        
        fun initialize(context: Context) {
            // 建立通知頻道
            createNotificationChannel(context)
            
            try {
                // 初始化 Firebase Messaging
                // 註冊 FCM token
                registerFCMToken(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize notification service", e)
            }
        }
        
        /**
         * 檢查是否有勿擾模式覆蓋權限
         */
        fun hasDndPermission(context: Context): Boolean {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notificationManager.isNotificationPolicyAccessGranted
            } else {
                true
            }
        }
        
        /**
         * 開啟勿擾模式權限設定頁面
         */
        fun openDndPermissionSettings(context: Context) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
        
        private fun registerFCMToken(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val token = FirebaseMessaging.getInstance().token.await()
                    
                    // 訂閱Firebase topics
                    subscribeToTopics()
                    
                    // FCM token 已獲取，可用於後端推播通知
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get FCM token", e)
                }
            }
        }
        
        private fun subscribeToTopics() {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 訂閱 'all' topic (廣播推播)
                    FirebaseMessaging.getInstance().subscribeToTopic("all").await()
                    
                    // 訂閱 'admin' topic (管理員推播)
                    FirebaseMessaging.getInstance().subscribeToTopic("admin").await()
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 訂閱Firebase topics失敗", e)
                }
            }
        }
        
        private fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                
                // 建立警告通知頻道 (IMPORTANCE_LOW)
                val warningChannel = NotificationChannel(
                    WARNING_CHANNEL_ID,
                    WARNING_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = WARNING_CHANNEL_DESCRIPTION
                    enableVibration(false)
                    enableLights(false)
                    setShowBadge(true)
                }
                
                // 建立危險通知頻道 (IMPORTANCE_DEFAULT)
                val dangerChannel = NotificationChannel(
                    DANGER_CHANNEL_ID,
                    DANGER_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = DANGER_CHANNEL_DESCRIPTION
                    enableVibration(true)
                    enableLights(true)
                    setShowBadge(true)
                }
                
                // 建立緊急通知頻道 (IMPORTANCE_HIGH) - 可繞過勿擾模式
                val criticalChannel = NotificationChannel(
                    CRITICAL_CHANNEL_ID,
                    CRITICAL_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CRITICAL_CHANNEL_DESCRIPTION
                    enableVibration(true)
                    enableLights(true)
                    setShowBadge(true)
                    // 設定為可繞過勿擾模式
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        setBypassDnd(true)
                    }
                }
                
                // 建立所有通知頻道
                notificationManager.createNotificationChannel(warningChannel)
                notificationManager.createNotificationChannel(dangerChannel)
                notificationManager.createNotificationChannel(criticalChannel)
            }
        }
    }
    
    fun showNotification(
        context: Context,
        title: String,
        message: String,
        severity: NotificationSeverity = NotificationSeverity.DANGER
    ) {
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 根據警報等級選擇對應的通知頻道
        val channelId = getChannelId(severity)
        
        // 如果是緊急通知，確保聲音能夠播放
        if (severity == NotificationSeverity.CRITICAL) {
            ensureCriticalAlertSound(context)
        }
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(getPriority(severity))
            .setDefaults(getDefaults(severity))
            .setCategory(getCategory(severity))
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = System.currentTimeMillis().toInt()
        
        notificationManager.notify(notificationId, notification)
        
        // 檢查通知是否啟用
        val areNotificationsEnabled = notificationManager.areNotificationsEnabled()
        
        if (!areNotificationsEnabled) {
            Log.w(TAG, "⚠️ 通知權限未授予！請在設置中啟用通知權限")
        }
        
        // 檢查勿擾模式權限
        if (severity == NotificationSeverity.CRITICAL && !hasDndPermission(context)) {
            Log.w(TAG, "⚠️ 緊急通知需要勿擾模式覆蓋權限！請在設置中授予權限")
        }
        
        Log.d(TAG, "📱 已發送 ${severity.name} 等級通知：$title")
    }
    
    /**
     * 處理來自 Firebase Cloud Messaging 的推播通知
     */
    fun handleFirebaseNotification(
        context: Context,
        title: String?,
        message: String?,
        data: Map<String, String>? = null
    ) {
        
        val notificationTitle = title ?: "Angle Caring 通知"
        val notificationMessage = message ?: "您收到一則新通知"
        val severity = data?.get("severity")?.let { 
            try {
                NotificationSeverity.valueOf(it)
            } catch (e: IllegalArgumentException) {
                NotificationSeverity.DANGER
            }
        } ?: NotificationSeverity.DANGER
        
        showNotification(context, notificationTitle, notificationMessage, severity)
    }
    
    private fun getPriority(severity: NotificationSeverity): Int {
        return when (severity) {
            NotificationSeverity.CRITICAL -> NotificationCompat.PRIORITY_MAX
            NotificationSeverity.DANGER -> NotificationCompat.PRIORITY_DEFAULT
            NotificationSeverity.WARNING -> NotificationCompat.PRIORITY_LOW
        }
    }
    
    private fun getDefaults(severity: NotificationSeverity): Int {
        return when (severity) {
            NotificationSeverity.CRITICAL -> NotificationCompat.DEFAULT_ALL
            NotificationSeverity.DANGER -> NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE
            NotificationSeverity.WARNING -> NotificationCompat.DEFAULT_SOUND
        }
    }
    
    /**
     * 根據警報等級獲取對應的通知頻道ID
     */
    private fun getChannelId(severity: NotificationSeverity): String {
        return when (severity) {
            NotificationSeverity.WARNING -> WARNING_CHANNEL_ID
            NotificationSeverity.DANGER -> DANGER_CHANNEL_ID
            NotificationSeverity.CRITICAL -> CRITICAL_CHANNEL_ID
        }
    }
    
    /**
     * 根據警報等級獲取通知分類
     */
    private fun getCategory(severity: NotificationSeverity): String {
        return when (severity) {
            NotificationSeverity.CRITICAL -> NotificationCompat.CATEGORY_ALARM
            NotificationSeverity.DANGER -> NotificationCompat.CATEGORY_STATUS
            NotificationSeverity.WARNING -> NotificationCompat.CATEGORY_STATUS
        }
    }
    
    /**
     * 確保緊急通知聲音能夠播放
     * 即使在靜音模式下也會播放聲音
     */
    private fun ensureCriticalAlertSound(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val originalRingerMode = audioManager.ringerMode
            
            // 如果設備處於靜音或震動模式，暫時切換到正常模式
            if (originalRingerMode != AudioManager.RINGER_MODE_NORMAL) {
                Log.d(TAG, "🔊 設備處於靜音模式，暫時切換到正常模式以播放緊急通知聲音")
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                
                // 延遲後恢復原始模式
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        audioManager.ringerMode = originalRingerMode
                        Log.d(TAG, "🔇 已恢復原始鈴聲模式")
                    } catch (e: Exception) {
                        Log.e(TAG, "恢復原始鈴聲模式失敗", e)
                    }
                }, 5000) // 5秒後恢復
            }
        } catch (e: Exception) {
            Log.e(TAG, "設定緊急通知聲音失敗", e)
        }
    }
}

enum class NotificationSeverity {
    CRITICAL,  // 緊急 - 使用 IMPORTANCE_HIGH + 繞過勿擾模式
    DANGER,    // 危險 - 使用 IMPORTANCE_DEFAULT  
    WARNING    // 警告 - 使用 IMPORTANCE_LOW
}
