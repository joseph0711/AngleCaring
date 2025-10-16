package com.example.anglecaring.ui.alarmhistory

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.anglecaring.data.api.AlarmSensorReadingsData
import com.example.anglecaring.data.model.AlarmEvent
import com.example.anglecaring.data.model.SensorReading
import com.example.anglecaring.data.repository.AlarmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CO2AlarmDetailState {
    object Loading : CO2AlarmDetailState()
    data class Success(
        val sensorReadings: List<SensorReading> = emptyList()
    ) : CO2AlarmDetailState()
    data class Error(val message: String) : CO2AlarmDetailState()
}

class CO2AlarmDetailViewModel : ViewModel() {
    private val TAG = "CO2AlarmDetailViewModel"
    
    private val alarmRepository = AlarmRepository()
    
    // 警報詳情狀態
    private val _detailState = MutableStateFlow<CO2AlarmDetailState>(CO2AlarmDetailState.Loading)
    val detailState: StateFlow<CO2AlarmDetailState> = _detailState.asStateFlow()
    
    // 載入相關感測器讀數
    fun loadSensorReadings(alarmId: Int) {
        viewModelScope.launch {
            try {
                _detailState.value = CO2AlarmDetailState.Loading
                Log.d(TAG, "開始載入二氧化碳感測器讀數，alarmId: $alarmId")
                
                // 使用新的 repository 方法獲取感測器讀數
                alarmRepository.getAlarmWithSensorReadings(alarmId).collect { result ->
                    result.fold(
                        onSuccess = { data ->
                            Log.d(TAG, "成功獲取二氧化碳感測器讀數數量: ${data.sensorReadings.size}")
                            data.sensorReadings.forEachIndexed { index, reading ->
                                Log.d(TAG, "讀數 $index: 時間=${reading.readingTime}, 數值=${reading.numericValue}")
                            }
                            
                            _detailState.value = CO2AlarmDetailState.Success(
                                sensorReadings = data.sensorReadings
                            )
                        },
                        onFailure = { exception ->
                            Log.e(TAG, "獲取二氧化碳感測器讀數失敗", exception)
                            _detailState.value = CO2AlarmDetailState.Error(
                                exception.message ?: "獲取感測器讀數時發生未知錯誤"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "載入二氧化碳感測器讀數時發生錯誤", e)
                _detailState.value = CO2AlarmDetailState.Error(e.message ?: "未知錯誤")
            }
        }
    }
} 