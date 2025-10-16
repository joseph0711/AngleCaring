package com.example.anglecaring

import android.app.Application
import android.util.Log
import com.example.anglecaring.services.FirebaseNotificationService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AngleCaringApplication : Application() {
    
    companion object {
        private const val TAG = "AngleCaringApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        initializeApp()
    }
    
    private fun initializeApp() {
        // Initialize any application-wide configurations here
        // This is a good place to initialize libraries, configure logging, etc.
        
        // Using try-catch to prevent app crashes on startup
        try {
            // Initialize Firebase Notification Service
            FirebaseNotificationService.initialize(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing application", e)
        }
    }
} 