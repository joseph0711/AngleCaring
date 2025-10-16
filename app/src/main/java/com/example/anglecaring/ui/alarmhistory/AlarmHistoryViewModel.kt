package com.example.anglecaring.ui.alarmhistory

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.anglecaring.data.model.AlarmEvent
import com.example.anglecaring.data.model.AlarmRiskLevel
import com.example.anglecaring.data.model.SensorReading
import com.example.anglecaring.data.repository.SensorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.Calendar
import kotlinx.coroutines.delay

sealed class AlarmHistoryState {
    object Loading : AlarmHistoryState()
    data class Success(val alarms: List<AlarmEvent>) : AlarmHistoryState()
    data class Error(val message: String) : AlarmHistoryState()
}

class AlarmHistoryViewModel : ViewModel() {
    
    private val _historyState = MutableStateFlow<AlarmHistoryState>(AlarmHistoryState.Loading)
    val historyState: StateFlow<AlarmHistoryState> = _historyState.asStateFlow()
    
    // 新增用於存儲原始警報數據的變數
    private var originalAlarms: List<AlarmEvent> = emptyList()
    
    // 目前選擇的風險級別過濾器
    private val _selectedRiskLevel = MutableStateFlow<String?>(null)
    val selectedRiskLevel: StateFlow<String?> = _selectedRiskLevel.asStateFlow()
    
    // 目前選擇的感應器過濾器
    private val _selectedSensor = MutableStateFlow<String?>(null)
    val selectedSensor: StateFlow<String?> = _selectedSensor.asStateFlow()
    
    // Add date range filter state
    private val _startDate = MutableStateFlow<Date?>(null)
    val startDate: StateFlow<Date?> = _startDate.asStateFlow()
    
    private val _endDate = MutableStateFlow<Date?>(null)
    val endDate: StateFlow<Date?> = _endDate.asStateFlow()
    
    // New StateFlow for sensor readings
    private val _sensorReadings = MutableStateFlow<Map<Int, List<SensorReading>>>(emptyMap())
    val sensorReadings: StateFlow<Map<Int, List<SensorReading>>> = _sensorReadings.asStateFlow()
    
    // Map to track expanded alarm items
    private val _expandedAlarms = MutableStateFlow<Set<Int>>(emptySet())
    val expandedAlarms: StateFlow<Set<Int>> = _expandedAlarms.asStateFlow()
    
    // Repository for sensor data
    private val sensorRepository = SensorRepository()
    
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
    
    private val TAG = "AlarmHistoryVM"

    private val baseUrl = com.example.anglecaring.BuildConfig.API_BASE_URL.removeSuffix("/")
    private val alarmsApiUrl = "$baseUrl/api/alarms"
    
    // 添加導航事件
    private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
    val navigationEvent: StateFlow<NavigationEvent?> = _navigationEvent.asStateFlow()
    
    init {
        loadAlarmHistory()
    }

    // 設置風險級別過濾器
    fun setRiskLevelFilter(riskLevel: String?) {
        _selectedRiskLevel.value = riskLevel
        applyFilters()
    }
    
    // 設置感應器過濾器
    fun setSensorFilter(sensor: String?) {
        _selectedSensor.value = sensor
        applyFilters()
    }
    
    // 應用過濾器
    private fun applyFilters() {
        var filtered = originalAlarms
        
        // 根據風險級別過濾
        if (_selectedRiskLevel.value != null) {
            filtered = filtered.filter { it.riskLevel == _selectedRiskLevel.value }
        }
        
        // 根據感應器過濾
        if (_selectedSensor.value != null) {
            filtered = filtered.filter { it.deviceName == _selectedSensor.value }
        }
        
        _historyState.value = AlarmHistoryState.Success(filtered)
    }
    
    fun loadAlarmHistory(userId: Int? = null) {
        viewModelScope.launch {
            _historyState.value = AlarmHistoryState.Loading
            
            try {
                val urlString = if (userId != null) {
                    "$alarmsApiUrl/user/$userId"
                } else {
                    alarmsApiUrl
                }
                
                Log.d(TAG, "Fetching alarm history from: $urlString")
                
                val response = withContext(Dispatchers.IO) {
                    try {
                        URL(urlString).readText()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to connect to alarm API endpoint", e)
                        return@withContext null
                    }
                }
                
                if (response != null) {
                    Log.d(TAG, "Alarm history response: $response")
                    
                    try {
                        val jsonResponse = JSONObject(response)
                        
                        if (jsonResponse.has("success") && jsonResponse.getBoolean("success")) {
                            val jsonArray = jsonResponse.getJSONArray("data")
                            val alarms = mutableListOf<AlarmEvent>()
                            
                            for (i in 0 until jsonArray.length()) {
                                val item = jsonArray.getJSONObject(i)
                                val alarm = AlarmEvent(
                                    alarmId = item.getInt("alarm_id"),
                                    userId = item.getInt("user_id"),
                                    alarmTime = item.getString("alarm_time"),
                                    alarmLabel = if (item.isNull("alarm_label")) null else item.getString("alarm_label"),
                                    riskLevel = item.getString("risk_level"),
                                    deviceId = if (item.isNull("device_id")) "unknown" else item.getString("device_id"),
                                    deviceLocation = if (item.isNull("device_location")) null else item.getString("device_location"),
                                    deviceName = if (item.isNull("device_name")) null else item.getString("device_name"),
                                    value = if (item.isNull("value")) null else item.getDouble("value").toFloat(),
                                    standardValue = if (item.isNull("standard_value")) null else item.getDouble("standard_value").toFloat()
                                )
                                alarms.add(alarm)
                            }
                            
                            // 保存原始數據
                            originalAlarms = alarms
                            
                            // 應用當前過濾器
                            applyFilters()
                        } else {
                            // API返回錯誤
                            val errorMessage = if (jsonResponse.has("message")) 
                                jsonResponse.getString("message") 
                                else "API returned error"
                            
                            _historyState.value = AlarmHistoryState.Error(errorMessage)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing alarm history response", e)
                        _historyState.value = AlarmHistoryState.Error("Error parsing API response: ${e.message}")
                    }
                } else {
                    // API連接失敗
                    _historyState.value = AlarmHistoryState.Error("Failed to connect to alarm API")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading alarm history", e)
                _historyState.value = AlarmHistoryState.Error("Error: ${e.message}")
            }
        }
    }
    
    fun refreshData() {
        loadAlarmHistory()
    }

    fun updateAlarmStatus(alarmId: Int, isActive: Boolean) {
        viewModelScope.launch {
            try {
                val currentState = _historyState.value
                if (currentState is AlarmHistoryState.Success) {



                    // Update server
                    updateAlarmStatusOnServer(alarmId, isActive)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating alarm status", e)
            }
        }
    }

    private fun updateAlarmStatusOnServer(alarmId: Int, isActive: Boolean) {
        viewModelScope.launch {
            try {
                val updateUrl = "$alarmsApiUrl/$alarmId/status"

                withContext(Dispatchers.IO) {
                    val connection = URL(updateUrl).openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "PUT"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.doOutput = true

                    // Create the JSON body
                    val jsonBody = JSONObject().apply {
                        put("isActive", isActive)
                    }

                    // Write to the connection
                    val outputStream = connection.outputStream
                    outputStream.write(jsonBody.toString().toByteArray())
                    outputStream.close()

                    val responseCode = connection.responseCode
                    Log.d(TAG, "Alarm status update response code: $responseCode")

                    // Read the response if needed
                    if (responseCode == 200) {
                        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                        Log.d(TAG, "Alarm status update response: $responseText")
                    } else {
                        val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                        Log.e(TAG, "Error updating alarm status: $errorText")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating alarm status", e)
            }
        }
    }

    // 清除所有過濾器
    fun clearFilters() {
        _selectedRiskLevel.value = null
        _selectedSensor.value = null
        _startDate.value = null
        _endDate.value = null
        if (originalAlarms.isNotEmpty()) {
            _historyState.value = AlarmHistoryState.Success(originalAlarms)
        }
    }
    
    // Add functions to set date range filters
    fun setStartDate(date: Date?) {
        _startDate.value = date
    }
    
    fun setEndDate(date: Date?) {
        _endDate.value = date
    }
    
    // Toggle the expanded state of an alarm
    fun toggleAlarmExpanded(alarmId: Int) {
        val currentExpanded = _expandedAlarms.value
        _expandedAlarms.value = if (currentExpanded.contains(alarmId)) {
            currentExpanded - alarmId
        } else {
            // When expanding an alarm, fetch the sensor readings if it's a CO alarm
            val alarm = (_historyState.value as? AlarmHistoryState.Success)?.alarms?.find { it.alarmId == alarmId }
            if (alarm?.deviceName == "CO") {
                fetchSensorReadingsForAlarm(alarm)
            }
            currentExpanded + alarmId
        }
    }
    
    // Fetch sensor readings for a specific alarm
    private fun fetchSensorReadingsForAlarm(alarm: AlarmEvent) {
        viewModelScope.launch {
            val deviceId = alarm.deviceId ?: return@launch
            
            // Calculate start time (12 hours before the alarm)
            val alarmTime = parseAlarmTime(alarm.alarmTime) ?: return@launch
            val calendar = Calendar.getInstance()
            calendar.time = alarmTime
            calendar.add(Calendar.HOUR, -12)
            val startTime = calendar.time
            
            // Fetch readings from repository
            sensorRepository.getCOReadingsForDevice(deviceId, startTime, alarmTime)
                .collect { readings ->
                    // Store the readings associated with this alarm ID
                    _sensorReadings.value = _sensorReadings.value + (alarm.alarmId to readings)
                }
        }
    }
    
    // Helper method to parse alarm time string
    private fun parseAlarmTime(timeString: String): Date? {
        return try {
            val cleanedTime = timeString
                .replace("T", " ")
                .replace("Z", "")
                .replace(Regex("\\.\\d+"), "")
            
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).parse(cleanedTime)
        } catch (e: Exception) {
            null
        }
    }

    
    // 導航到警報詳情頁面
    fun viewAlarmDetail(alarmId: Int) {
        Log.d(TAG, "viewAlarmDetail 被調用，警報ID: $alarmId")
        _navigationEvent.value = NavigationEvent.NavigateToAlarmDetail(alarmId)
    }
    
    // 重置導航事件
    fun resetNavigationEvent() {
        _navigationEvent.value = null
    }
    
    // 導航事件封裝類
    sealed class NavigationEvent {
        data class NavigateToAlarmDetail(val alarmId: Int) : NavigationEvent()
    }
} 