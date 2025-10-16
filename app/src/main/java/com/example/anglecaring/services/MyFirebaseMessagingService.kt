package com.example.anglecaring.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    
    companion object {
        private const val TAG = "MyFirebaseMessagingService"
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // 檢查消息是否包含數據負載
        if (remoteMessage.data.isNotEmpty()) {
            // Handle data payload if needed
        }
        
        // 檢查消息是否包含通知負載
        remoteMessage.notification?.let {
            
            // 處理通知
            FirebaseNotificationService().handleFirebaseNotification(
                this,
                it.title,
                it.body,
                remoteMessage.data
            )
        }
    }
    
    override fun onNewToken(token: String) {
        // 將新的 FCM token 發送到後端服務器
        sendRegistrationToServer(token)
    }
    
    private fun sendRegistrationToServer(token: String) {
        // FCM token 已獲取，後端可直接使用 Firebase Admin SDK 發送推播通知
        Log.d(TAG, "FCM Token: $token")
    }
}
