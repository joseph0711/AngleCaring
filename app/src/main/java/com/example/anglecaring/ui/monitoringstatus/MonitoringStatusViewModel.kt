package com.example.anglecaring.ui.monitoringstatus

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

sealed class MonitoringStatusState {
    object Loading : MonitoringStatusState()
    data class Success(val summary: MonitoringStatusSummary) : MonitoringStatusState()
    data class Error(val message: String) : MonitoringStatusState()
}

class MonitoringStatusViewModel : ViewModel() {
    private val _historyState = MutableStateFlow<MonitoringStatusState>(MonitoringStatusState.Loading)
    val historyState: StateFlow<MonitoringStatusState> = _historyState.asStateFlow()
    
    private val apiService = RetrofitClient.apiService

    fun loadMonitoringStatus(userId: Int) {
        viewModelScope.launch {
            try {
                _historyState.value = MonitoringStatusState.Loading
                
                // 調用真實的API
                val response = apiService.getMonitoringStatusSummary(userId)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.data != null) {
                        _historyState.value = MonitoringStatusState.Success(body.data)
                    } else {
                        _historyState.value = MonitoringStatusState.Error("獲取監控狀態數據失敗")
                    }
                } else {
                    _historyState.value = MonitoringStatusState.Error("API 請求失敗: ${response.code()}")
                }
            } catch (e: Exception) {
                _historyState.value = MonitoringStatusState.Error("載入監控狀態失敗: ${e.message}")
            }
        }
    }

    fun refreshData(userId: Int) {
        loadMonitoringStatus(userId)
    }
}
