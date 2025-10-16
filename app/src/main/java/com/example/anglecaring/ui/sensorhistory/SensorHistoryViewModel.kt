package com.example.anglecaring.ui.sensorhistory

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.util.TimeZone

data class SensorReading(
    val id: Int,
    val deviceId: String,
    val readingTime: Date,
    val booleanValue: Boolean,
    val numericValue: Float?,
    val createdAt: Date
)

sealed class SensorHistoryState {
    object Loading : SensorHistoryState()
    data class Success(val readings: List<SensorReading>) : SensorHistoryState()
    data class Error(val message: String) : SensorHistoryState()
}

class SensorHistoryViewModel : ViewModel() {
    
    private val _historyState = MutableStateFlow<SensorHistoryState>(SensorHistoryState.Loading)
    val historyState: StateFlow<SensorHistoryState> = _historyState
    
    private val _selectedDeviceType = MutableStateFlow<String?>(null)
    val selectedDeviceType: StateFlow<String?> = _selectedDeviceType
    
    private val _selectedLocation = MutableStateFlow<String?>(null)
    val selectedLocation: StateFlow<String?> = _selectedLocation
    
    // Add date range filter state
    private val _startDate = MutableStateFlow<Date?>(null)
    val startDate: StateFlow<Date?> = _startDate
    
    private val _endDate = MutableStateFlow<Date?>(null)
    val endDate: StateFlow<Date?> = _endDate
    
    // Date parsers that support different formats including ISO-8601
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    // Add a format that treats Z as a timezone indicator rather than a literal 'Z'
    private val isoDateFormatWithZ = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
    private val legacyDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    // Add format for the database timestamp with timezone
    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSSS XXX", Locale.getDefault())
    // Add fallback formats for specific database records
    private val fallbackDbFormat1 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSSS", Locale.getDefault())
    private val fallbackDbFormat2 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    private val TAG = "SensorHistoryVM" // Tag for logging

    private val baseUrl = com.example.anglecaring.BuildConfig.API_BASE_URL.removeSuffix("/")
    private val sensorsApiUrl = "$baseUrl/api/sensors"
    
    init {
        loadSensorHistory()
    }
    
    // Parse date string using multiple formats
    private fun parseDate(dateStr: String): Date {
        Log.d(TAG, "Attempting to parse date: $dateStr")

        return try {
            // Try the ISO format with Z timezone indicator first
            isoDateFormatWithZ.parse(dateStr)?.also {
                Log.d(TAG, "Successfully parsed with isoDateFormatWithZ")
            } ?:
            // Try the database format
            dbDateFormat.parse(dateStr)?.also {
                Log.d(TAG, "Successfully parsed with dbDateFormat")
            } ?:
            // Try fallback database formats
            fallbackDbFormat1.parse(dateStr)?.also {
                Log.d(TAG, "Successfully parsed with fallbackDbFormat1")
            } ?:
            fallbackDbFormat2.parse(dateStr)?.also {
                Log.d(TAG, "Successfully parsed with fallbackDbFormat2")
            } ?:
            // Then try ISO format with quoted Z
            isoDateFormat.parse(dateStr)?.also {
                Log.d(TAG, "Successfully parsed with isoDateFormat")
            } ?:
            // Then try legacy format
            legacyDateFormat.parse(dateStr)?.also {
                Log.d(TAG, "Successfully parsed with legacyDateFormat")
            } ?:
            // Default to current date if all parsers fail
            Date().also {
                Log.w(TAG, "Failed to parse date, using current date as fallback")
            }
        } catch (e: Exception) {
            try {
                isoDateFormatWithZ.parse(dateStr) ?: Date()
            } catch (e: Exception) {
                try {
                    isoDateFormat.parse(dateStr) ?: Date()
                } catch (e: Exception) {
                    try {
                        legacyDateFormat.parse(dateStr) ?: Date()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse date: $dateStr", e)
                        Date()
                    }
                }
            }
        }
    }
    
    fun loadSensorHistory(deviceId: String? = null) {
        viewModelScope.launch {
            _historyState.value = SensorHistoryState.Loading
            
            try {
                val urlString = if (deviceId != null) {
                    "$sensorsApiUrl/$deviceId"
                } else {
                    sensorsApiUrl
                }
                
                Log.d(TAG, "Fetching sensor history from: $urlString")
                
                val response = withContext(Dispatchers.IO) {
                    try {
                        URL(urlString).readText()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to connect to sensor API endpoint", e)
                        return@withContext null
                    }
                }
                
                if (response != null) {
                    Log.d(TAG, "Sensor history response: $response")
                    
                    try {
                        val jsonResponse = JSONObject(response)
                        
                        if (jsonResponse.has("success") && jsonResponse.getBoolean("success")) {
                            val jsonArray = jsonResponse.getJSONArray("data")
                            val readings = mutableListOf<SensorReading>()
                            
                            for (i in 0 until jsonArray.length()) {
                                val item = jsonArray.getJSONObject(i)
                                try {
                                    val reading = SensorReading(
                                        id = item.getInt("reading_id"),
                                        deviceId = item.getString("device_id"),
                                        readingTime = parseDate(item.getString("reading_time")),
                                        booleanValue = item.optInt("boolean_value", 0) == 1,
                                        numericValue = if (item.has("numeric_value") && !item.isNull("numeric_value")) 
                                            item.getDouble("numeric_value").toFloat() else null,
                                        createdAt = parseDate(item.getString("created_at"))
                                    )
                                    readings.add(reading)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing sensor reading item: $item", e)
                                    // Continue to next item instead of failing entirely
                                }
                            }
                            
                            _historyState.value = SensorHistoryState.Success(readings)
                        } else {
                            // API返回錯誤
                            val errorMessage = if (jsonResponse.has("message")) 
                                jsonResponse.getString("message") 
                                else "API returned error"
                            
                            _historyState.value = SensorHistoryState.Error(errorMessage)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing sensor history response", e)
                        _historyState.value = SensorHistoryState.Error("Error parsing API response: ${e.message}")
                    }
                } else {
                    // API連接失敗
                    _historyState.value = SensorHistoryState.Error("Failed to connect to sensor API")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading sensor history", e)
                _historyState.value = SensorHistoryState.Error("Error: ${e.message}")
            }
        }
    }
    
    fun refreshData() {
        loadSensorHistory()
    }
    
    fun filterByDevice(deviceId: String) {
        loadSensorHistory(deviceId)
    }
    
    // Device type filter methods
    fun setDeviceTypeFilter(deviceType: String?) {
        _selectedDeviceType.value = deviceType
    }
    
    // Location filter methods  
    fun setLocationFilter(location: String?) {
        _selectedLocation.value = location
    }
    
    // Add functions to set date range filters
    fun setStartDate(date: Date?) {
        _startDate.value = date
    }
    
    fun setEndDate(date: Date?) {
        _endDate.value = date
    }
    
    // Clear all filters
    fun clearFilters() {
        _selectedDeviceType.value = null
        _selectedLocation.value = null
        _startDate.value = null
        _endDate.value = null
    }
} 
