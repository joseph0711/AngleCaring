package com.example.anglecaring.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import retrofit2.HttpException
import java.io.IOException

private const val TAG = "NetworkUtils"

/**
 * Determines if the device has an active internet connection
 */
fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    } else {
        @Suppress("DEPRECATION")
        val networkInfo = connectivityManager.activeNetworkInfo
        @Suppress("DEPRECATION")
        return networkInfo != null && networkInfo.isConnected
    }
}

/**
 * Handles common network exceptions and returns user-friendly error messages
 */
fun handleNetworkError(error: Throwable): String {
    Log.e(TAG, "Network error: ", error)
    
    return when (error) {
        is UnknownHostException -> "Cannot connect to server. Please check your internet connection."
        is SocketTimeoutException -> "Connection timed out. Please try again."
        is HttpException -> when (error.code()) {
            401 -> "Authentication failed. Please check your credentials."
            403 -> "You don't have permission to access this resource."
            404 -> "Resource not found."
            500 -> "Server error. Please try again later."
            else -> "Network error: ${error.message()}"
        }
        is IOException -> "Network error. Please check your internet connection."
        else -> "An unexpected error occurred: ${error.message}"
    }
}

/**
 * Validates email format
 */
fun isValidEmail(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

/**
 * Validates password strength
 * (minimum 6 characters, at least one letter and one number)
 */
fun isValidPassword(password: String): Boolean {
    if (password.length < 6) return false
    
    val hasLetter = password.any { it.isLetter() }
    val hasDigit = password.any { it.isDigit() }
    
    return hasLetter && hasDigit
} 