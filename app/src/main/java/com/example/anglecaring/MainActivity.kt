package com.example.anglecaring

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.anglecaring.data.api.RetrofitClient
import com.example.anglecaring.data.local.SessionManager
import com.example.anglecaring.ui.login.AuthViewModel
import com.example.anglecaring.ui.navigation.MainNavigation
import com.example.anglecaring.ui.theme.AngleCaringTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize RetrofitClient with application context
        RetrofitClient.initialize(applicationContext)
        
        // Initialize SessionManager and restore token if user is logged in
        val sessionManager = SessionManager(applicationContext)
        if (sessionManager.isLoggedIn()) {
            sessionManager.getAuthToken()?.let { token ->
                if (token.isNotEmpty()) {
                    RetrofitClient.setAuthToken(token)
                }
            }
        }
        
        enableEdgeToEdge()
        setContent {
            val authViewModel: AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            
            AngleCaringTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(
                        authViewModel = authViewModel,
                        onLogout = {
                            Toast.makeText(
                                this@MainActivity,
                                "已成功登出",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
    }
}