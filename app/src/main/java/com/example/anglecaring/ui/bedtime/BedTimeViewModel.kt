package com.example.anglecaring.ui.bedtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.anglecaring.data.api.request.BedTimeSettingsRequest
import com.example.anglecaring.data.model.BedTimeSettings
import com.example.anglecaring.data.repository.BedTimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BedTimeViewModel @Inject constructor(
    private val bedTimeRepository: BedTimeRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BedTimeUiState())
    val uiState: StateFlow<BedTimeUiState> = _uiState.asStateFlow()
    
    fun loadBedTimeSettings(userId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            bedTimeRepository.getBedTimeSettings(userId)
                .onSuccess { settings ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        bedTimeSettings = settings,
                        goToBedTime = settings?.goToBedTime ?: "22:00:00",
                        wakeUpTime = settings?.wakeUpTime ?: "07:00:00",
                        isActive = settings?.isActive ?: true,
                        alertIfLate = settings?.alertIfLate ?: false,
                        toleranceMinutes = settings?.toleranceMinutes ?: 15,
                        error = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
        }
    }
    
    fun updateGoToBedTime(time: String) {
        _uiState.value = _uiState.value.copy(goToBedTime = time)
    }
    
    fun updateWakeUpTime(time: String) {
        _uiState.value = _uiState.value.copy(wakeUpTime = time)
    }
    
    fun updateIsActive(isActive: Boolean) {
        _uiState.value = _uiState.value.copy(isActive = isActive)
    }
    
    fun updateAlertIfLate(alertIfLate: Boolean) {
        _uiState.value = _uiState.value.copy(alertIfLate = alertIfLate)
    }
    
    fun updateToleranceMinutes(minutes: Int) {
        _uiState.value = _uiState.value.copy(toleranceMinutes = minutes)
    }
    
    fun saveBedTimeSettings(userId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val request = BedTimeSettingsRequest(
                goToBedTime = _uiState.value.goToBedTime,
                wakeUpTime = _uiState.value.wakeUpTime,
                isActive = _uiState.value.isActive,
                alertIfLate = _uiState.value.alertIfLate,
                toleranceMinutes = _uiState.value.toleranceMinutes
            )
            
            val result = if (_uiState.value.bedTimeSettings != null) {
                bedTimeRepository.updateBedTimeSettings(userId, request)
            } else {
                bedTimeRepository.createBedTimeSettings(userId, request)
            }
            
            result
                .onSuccess { settings ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        bedTimeSettings = settings,
                        showSuccessMessage = true
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
        }
    }
    
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(showSuccessMessage = false)
    }
}

data class BedTimeUiState(
    val isLoading: Boolean = false,
    val bedTimeSettings: BedTimeSettings? = null,
    val goToBedTime: String = "22:00:00",
    val wakeUpTime: String = "07:00:00",
    val isActive: Boolean = true,
    val alertIfLate: Boolean = false,
    val toleranceMinutes: Int = 15,
    val error: String? = null,
    val showSuccessMessage: Boolean = false
)