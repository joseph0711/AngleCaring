package com.example.anglecaring.ui.familygroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.anglecaring.data.local.SessionManager
import com.example.anglecaring.data.model.FamilyGroup
import com.example.anglecaring.data.model.FamilyGroupMember
import com.example.anglecaring.data.repository.FamilyGroupDetailsData
import com.example.anglecaring.data.repository.FamilyGroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FamilyGroupViewModel @Inject constructor(
    private val familyGroupRepository: FamilyGroupRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamilyGroupUiState())
    val uiState: StateFlow<FamilyGroupUiState> = _uiState.asStateFlow()

    private val _groups = MutableStateFlow<List<FamilyGroup>>(emptyList())
    val groups: StateFlow<List<FamilyGroup>> = _groups.asStateFlow()

    private val _selectedGroupDetails = MutableStateFlow<FamilyGroupDetailsData?>(null)
    val selectedGroupDetails: StateFlow<FamilyGroupDetailsData?> = _selectedGroupDetails.asStateFlow()

    init {
        loadFamilyGroups()
    }

    fun loadFamilyGroups() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // 檢查用戶是否為一般用戶（非管理員）
            val currentUser = sessionManager.getUser()
            if (currentUser?.isAdmin == true) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "系統管理員無法使用家庭群組功能"
                )
                return@launch
            }
            
            familyGroupRepository.getFamilyGroups()
                .onSuccess { groupList ->
                    _groups.value = groupList
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "載入群組失敗"
                    )
                }
        }
    }

    fun createGroup(groupName: String, description: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            familyGroupRepository.createFamilyGroup(groupName, description)
                .onSuccess { newGroup ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "群組創建成功"
                    )
                    loadFamilyGroups() // 重新載入群組列表
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "創建群組失敗"
                    )
                }
        }
    }

    fun loadGroupDetails(groupId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            familyGroupRepository.getFamilyGroupDetails(groupId)
                .onSuccess { details ->
                    _selectedGroupDetails.value = details
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "載入群組詳情失敗"
                    )
                }
        }
    }

    fun updateGroup(groupId: Int, groupName: String, description: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            familyGroupRepository.updateFamilyGroup(groupId, groupName, description)
                .onSuccess { updatedGroup ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "群組資訊更新成功"
                    )
                    loadGroupDetails(groupId) // 重新載入群組詳情
                    loadFamilyGroups() // 重新載入群組列表
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "更新群組失敗"
                    )
                }
        }
    }

    fun deleteGroup(groupId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            familyGroupRepository.deleteFamilyGroup(groupId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "群組刪除成功"
                    )
                    _selectedGroupDetails.value = null
                    loadFamilyGroups() // 重新載入群組列表
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "刪除群組失敗"
                    )
                }
        }
    }

    fun addMember(groupId: Int, email: String, role: String = "member") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            familyGroupRepository.addMember(groupId, email, role)
                .onSuccess { newMember ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "成員添加成功"
                    )
                    loadGroupDetails(groupId) // 重新載入群組詳情以更新成員列表
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "添加成員失敗"
                    )
                }
        }
    }

    fun removeMember(groupId: Int, memberId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            familyGroupRepository.removeMember(groupId, memberId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "成員移除成功"
                    )
                    loadGroupDetails(groupId) // 重新載入群組詳情以更新成員列表
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "移除成員失敗"
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun clearSelectedGroupDetails() {
        _selectedGroupDetails.value = null
    }

    fun getCurrentUser() = sessionManager.getUser()
}

data class FamilyGroupUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
