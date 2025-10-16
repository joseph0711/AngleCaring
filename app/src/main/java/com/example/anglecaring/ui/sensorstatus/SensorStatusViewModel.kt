package com.example.anglecaring.ui.sensorstatus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.anglecaring.data.model.*
import com.example.anglecaring.data.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Calendar

sealed class SensorStatusState {
    object Loading : SensorStatusState()
    data class Success(val summary: SensorStatusSummary) : SensorStatusState()
    data class Error(val message: String) : SensorStatusState()
}

class SensorStatusViewModel : ViewModel() {
    private val _historyState = MutableStateFlow<SensorStatusState>(SensorStatusState.Loading)
    val historyState: StateFlow<SensorStatusState> = _historyState.asStateFlow()
    
    private val apiService = RetrofitClient.apiService

    fun loadSensorStatus(userId: Int) {
        viewModelScope.launch {
            try {
                _historyState.value = SensorStatusState.Loading
                
                // 調用真實的API
                val response = apiService.getSensorStatusSummary(userId)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.data != null) {
                        _historyState.value = SensorStatusState.Success(body.data)
                    } else {
                        _historyState.value = SensorStatusState.Error("獲取感應器狀態數據失敗")
                    }
                } else {
                    _historyState.value = SensorStatusState.Error("API 請求失敗: ${response.code()}")
                }
            } catch (e: Exception) {
                _historyState.value = SensorStatusState.Error("載入感應器狀態失敗: ${e.message}")
            }
        }
    }

    fun refreshData(userId: Int) {
        loadSensorStatus(userId)
    }
}
