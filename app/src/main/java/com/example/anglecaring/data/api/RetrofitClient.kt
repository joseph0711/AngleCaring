package com.example.anglecaring.data.api

import android.content.Context
import android.util.Log
import com.example.anglecaring.data.local.SessionManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // Backend URL configuration
    // Now using Azure cloud service - configured via BuildConfig
    // For local development, change API_BASE_URL in build.gradle.kts
    const val BASE_URL = com.example.anglecaring.BuildConfig.API_BASE_URL
    private const val TAG = "RetrofitClient"
    
    // Session manager
    private lateinit var sessionManager: SessionManager
    
    // Initialize with application context
    fun initialize(context: Context) {
        sessionManager = SessionManager(context)
    }
    
    // Get application context
    fun getApplicationContext(): Context? {
        return if (this::sessionManager.isInitialized) {
            sessionManager.getApplicationContext()
        } else {
            Log.e(TAG, "SessionManager not initialized")
            null
        }
    }
    
    // Update auth token
    fun setAuthToken(token: String) {
        if (this::sessionManager.isInitialized) {
            sessionManager.saveAuthToken(token)
        } else {
            Log.e(TAG, "Cannot save token: SessionManager not initialized")
        }
    }
    
    // Authentication interceptor to add token to all requests
    private class AuthInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            
            // If we have a sessionManager initialized, get the token from it
            val token = if (RetrofitClient::sessionManager.isInitialized) {
                sessionManager.getAuthToken()
            } else {
                Log.e(TAG, "SessionManager not initialized when trying to get token")
                null
            }
            
            // If we have an auth token, add it to the request
            val authRequest = if (!token.isNullOrEmpty()) {
                request.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                Log.w(TAG, "No auth token available for request to: ${request.url}")
                request
            }
            
            return chain.proceed(authRequest)
        }
    }
    
    // Custom date deserializer for handling date formats from the server
    private class DateDeserializer : JsonDeserializer<Date> {
        // Support multiple date formats including ISO-8601
        private val formats = arrayOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
        )
        
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): Date? {
            val dateStr = json.asString
            
            if (dateStr.isEmpty()) return null
            
            // Try all formats until one works
            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.getDefault())
                    return sdf.parse(dateStr)
                } catch (e: Exception) {
                    // Continue to the next format
                }
            }
            
            // If all formats fail, log the error and return null
            return null
        }
    }
    
    // LocalTime type adapter for converting between string and LocalTime
    private class LocalTimeAdapter : JsonSerializer<LocalTime>, JsonDeserializer<LocalTime> {
        private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        
        override fun serialize(
            src: LocalTime?,
            typeOfSrc: Type,
            context: com.google.gson.JsonSerializationContext
        ): JsonElement {
            return JsonPrimitive(formatter.format(src))
        }
        
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): LocalTime {
            val timeStr = json?.asString
            if (timeStr.isNullOrEmpty()) {
                return LocalTime.of(0, 0)
            }
            
            // Log the raw time string for debugging
            Log.d("RetrofitClient", "Deserializing LocalTime from: $timeStr")
            
            // First, try to parse ISO datetime strings (contains T and possibly Z)
            if (timeStr.contains('T')) {
                try {
                    // For ISO datetime strings like "1970-01-01T22:30:00.000Z"
                    val dateTime = LocalDateTime.parse(
                        timeStr.replace("Z", ""), 
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                    )
                    return dateTime.toLocalTime()
                } catch (e: Exception) {
                    try {
                        // For ISO datetime strings without milliseconds
                        val dateTime = LocalDateTime.parse(
                            timeStr.replace("Z", ""), 
                            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                        )
                        return dateTime.toLocalTime()
                    } catch (e: Exception) {
                        // Continue to other formats
                    }
                }
            }
            
            // Try regular time formats
            val formatters = arrayOf(
                DateTimeFormatter.ofPattern("HH:mm:ss"),
                DateTimeFormatter.ofPattern("HH:mm"),
                DateTimeFormatter.ISO_LOCAL_TIME
            )
            
            for (formatter in formatters) {
                try {
                    return LocalTime.parse(timeStr, formatter)
                } catch (e: DateTimeParseException) {
                    // Try next format
                }
            }
            
            // If all formats fail, log and return midnight
            Log.w("RetrofitClient", "Failed to parse time: $timeStr, defaulting to 00:00")
            return LocalTime.of(0, 0)
        }
    }
    
    // LocalDateTime type adapter for handling createdAt and updatedAt fields
    private class LocalDateTimeAdapter : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        
        override fun serialize(
            src: LocalDateTime?,
            typeOfSrc: Type,
            context: com.google.gson.JsonSerializationContext
        ): JsonElement {
            return src?.let { JsonPrimitive(formatter.format(it)) } ?: JsonPrimitive("")
        }
        
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): LocalDateTime {
            val dateStr = json?.asString
            if (dateStr.isNullOrEmpty()) {
                return LocalDateTime.now()
            }
            
            // Try different formats
            val formatters = arrayOf(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ISO_DATE_TIME
            )
            
            for (formatter in formatters) {
                try {
                    return LocalDateTime.parse(dateStr, formatter)
                } catch (e: DateTimeParseException) {
                    // Try next format
                }
            }
            
            // If all formats fail, return current time
            return LocalDateTime.now()
        }
    }
    
    // Create Gson instance with custom date handling
    private val gson: Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .registerTypeAdapter(Date::class.java, DateDeserializer())
        .registerTypeAdapter(LocalTime::class.java, LocalTimeAdapter())
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
        .create()
    
    // Configure OkHttpClient with logging and timeouts
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .addInterceptor(AuthInterceptor())
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // Build Retrofit instance with forced recompilation
    val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    
    // Create API service interface
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
} 