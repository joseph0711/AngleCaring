package com.example.anglecaring.ui.common

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Toast 管理組件，避免重複顯示相同的 Toast 消息
 */
@Composable
fun ToastManager(
    error: String?,
    successMessage: String?,
    onErrorCleared: () -> Unit,
    onSuccessCleared: () -> Unit
) {
    val context = LocalContext.current
    var lastError by remember { mutableStateOf<String?>(null) }
    var lastSuccess by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(error) {
        error?.let { currentError ->
            // 只有當錯誤消息與上次不同時才顯示
            if (currentError != lastError) {
                Toast.makeText(context, currentError, Toast.LENGTH_LONG).show()
                lastError = currentError
                onErrorCleared()
            }
        }
    }
    
    LaunchedEffect(successMessage) {
        successMessage?.let { currentSuccess ->
            // 只有當成功消息與上次不同時才顯示
            if (currentSuccess != lastSuccess) {
                Toast.makeText(context, currentSuccess, Toast.LENGTH_SHORT).show()
                lastSuccess = currentSuccess
                onSuccessCleared()
            }
        }
    }
}
