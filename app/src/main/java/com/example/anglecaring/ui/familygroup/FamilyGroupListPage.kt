package com.example.anglecaring.ui.familygroup

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.anglecaring.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.anglecaring.data.model.FamilyGroup
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalContext
import com.example.anglecaring.ui.common.ToastManager
import com.example.anglecaring.ui.common.ErrorState
import com.example.anglecaring.ui.common.AdminBadge

// 群組篩選類型
enum class GroupFilter(val displayName: String) {
    ALL("全部"),
    ADMIN("我管理的"),
    MEMBER("我參與的")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyGroupListPage(
    onNavigateToGroupDetails: (Int) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: FamilyGroupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    
    // 搜索和篩選狀態
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(GroupFilter.ALL) }
    
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // 獲取 Context 用於顯示 Toast
    val context = LocalContext.current
    
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadFamilyGroups()
    }

    // 使用 ToastManager 管理 Toast 顯示
    ToastManager(
        error = uiState.error,
        successMessage = uiState.successMessage,
        onErrorCleared = { viewModel.clearError() },
        onSuccessCleared = { viewModel.clearSuccessMessage() }
    )
    
    // 篩選群組列表
    val filteredGroups = remember(groups, searchQuery, selectedFilter) {
        groups.filter { group ->
            val matchesSearch = searchQuery.isEmpty() || 
                group.groupName.contains(searchQuery, ignoreCase = true) ||
                (group.description?.contains(searchQuery, ignoreCase = true) == true)
            
            val matchesFilter = when (selectedFilter) {
                GroupFilter.ALL -> true
                GroupFilter.ADMIN -> group.role == "admin"
                GroupFilter.MEMBER -> group.role != "admin"
            }
            
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("家庭群組", fontWeight = FontWeight.Bold)
                        Text(
                            text = "共 ${filteredGroups.size} 個群組",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                painter = painterResource(id = R.drawable.arrow_back_24dp),
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                actions = {
                    // 重新整理按鈕
                    IconButton(onClick = { viewModel.loadFamilyGroups() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.refresh_24dp),
                            contentDescription = "重新整理",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.add_24dp),
                    contentDescription = "創建群組"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 搜索欄
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { keyboardController?.hide() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            
            // 篩選標籤
            FilterChips(
                selectedFilter = selectedFilter,
                onFilterChange = { selectedFilter = it },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // 內容區域
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "載入群組資料中...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (uiState.error != null) {
                ErrorState(
                    error = uiState.error ?: "未知錯誤",
                    onRetry = { viewModel.loadFamilyGroups() },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (filteredGroups.isEmpty()) {
                EmptyState(
                    message = if (searchQuery.isNotEmpty() || selectedFilter != GroupFilter.ALL) 
                        "沒有找到符合條件的群組" 
                    else "暫無群組資料",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = filteredGroups.withIndex().toList(),
                        key = { (index, group) -> 
                            "group_${index}_${group.groupId}_${group.groupName}"
                        }
                    ) { (index, group) ->
                        FamilyGroupManagementCard(
                            group = group,
                            onNavigateToDetails = { onNavigateToGroupDetails(group.groupId) }
                        )
                    }
                }
            }
        }
    }

    // 創建群組對話框
    if (showCreateDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { groupName, description ->
                viewModel.createGroup(groupName, description)
                showCreateDialog = false
            }
        )
    }
}

// 搜索欄組件
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("搜尋群組名稱") },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.search_24dp),
                contentDescription = "搜尋"
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        painter = painterResource(id = R.drawable.close_24dp),
                        contentDescription = "清除"
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        shape = RoundedCornerShape(12.dp)
    )
}

// 篩選標籤組件
@Composable
fun FilterChips(
    selectedFilter: GroupFilter,
    onFilterChange: (GroupFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GroupFilter.values().forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterChange(filter) },
                label = { Text(filter.displayName) },
                leadingIcon = if (selectedFilter == filter) {
                    { Icon(painter = painterResource(id = R.drawable.check_24dp), contentDescription = null) }
                } else null
            )
        }
    }
}


// 空狀態組件
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.group_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// 群組管理卡片組件
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyGroupManagementCard(
    group: FamilyGroup,
    onNavigateToDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToDetails() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 群組圖標
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.group_24dp),
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(12.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 群組信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = group.groupName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (group.role == "admin") {
                        AdminBadge()
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                if (!group.description.isNullOrBlank()) {
                    Text(
                        text = group.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                Text(
                    text = "創建者: ${group.creatorName ?: "未知"} • ${formatDate(group.createdTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 操作按鈕
            IconButton(onClick = onNavigateToDetails) {
                Icon(
                    painter = painterResource(id = R.drawable.info_24dp),
                    contentDescription = "查看詳情",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


// 日期格式化函數
private fun formatDate(date: Date?): String {
    return if (date != null) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date)
    } else {
        "未知"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyGroupCard(
    group: FamilyGroup,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(onClick = onClick)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                        ),
                        startY = 0f,
                        endY = 200f
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // 標題區域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.group_24dp),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(8.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = group.groupName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                if (!group.description.isNullOrBlank()) {
                                    Text(
                                        text = group.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // 角色標籤
                    if (group.role != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (group.role == "admin") {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.person_24dp),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (group.role == "admin") {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when (group.role) {
                                        "admin" -> "管理員"
                                        else -> "成員"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (group.role == "admin") {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 詳細信息區域
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (group.role == "admin") "創建者" else "邀請者",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = group.creatorName ?: "未知",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        group.createdTime?.let { date ->
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "創建時間",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.clip(RoundedCornerShape(28.dp))
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // 標題區域
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.group_24dp),
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(8.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = "創建家庭群組",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "與家人共享照護資訊",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 表單區域
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        OutlinedTextField(
                            value = groupName,
                            onValueChange = { 
                                groupName = it
                                isError = it.isBlank()
                            },
                            label = { Text("群組名稱") },
                            placeholder = { Text("例如：王家大家庭") },
                            isError = isError,
                            supportingText = if (isError) {
                                { 
                                    Text(
                                        "請輸入群組名稱",
                                        color = MaterialTheme.colorScheme.error
                                    ) 
                                }
                            } else {
                                { 
                                    Text(
                                        "必填欄位",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ) 
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("群組描述") },
                            placeholder = { Text("簡短描述這個群組的用途...") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            supportingText = {
                                Text(
                                    "選填欄位",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 按鈕區域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            text = "取消",
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    ElevatedButton(
                        onClick = {
                            if (groupName.isNotBlank()) {
                                onConfirm(
                                    groupName.trim(),
                                    if (description.isBlank()) null else description.trim()
                                )
                            } else {
                                isError = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "創建群組",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
