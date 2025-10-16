package com.example.anglecaring.ui.familygroup

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.anglecaring.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.anglecaring.data.model.FamilyGroup
import com.example.anglecaring.data.model.FamilyGroupMember
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext
import com.example.anglecaring.ui.common.ToastManager
import com.example.anglecaring.ui.common.ErrorState
import com.example.anglecaring.ui.common.AdminBadge
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyGroupDetailsPage(
    groupId: Int,
    onNavigateBack: () -> Unit,
    viewModel: FamilyGroupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val groupDetails by viewModel.selectedGroupDetails.collectAsStateWithLifecycle()
    
    // 獲取當前用戶信息
    val currentUser = remember { viewModel.getCurrentUser() }
    
    // 獲取 Context 用於顯示 Toast
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // 狀態管理
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showEditGroupDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var memberToRemove by remember { mutableStateOf<FamilyGroupMember?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    
    // 跟蹤操作狀態，區分載入錯誤和操作錯誤
    var isPerformingOperation by remember { mutableStateOf(false) }
    var hasInitiallyLoaded by remember { mutableStateOf(false) }
    var isAddingMember by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) {
        hasInitiallyLoaded = false
        viewModel.loadGroupDetails(groupId)
    }

    // 監聽載入狀態變化
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && groupDetails != null) {
            hasInitiallyLoaded = true
        }
        if (!uiState.isLoading) {
            // 操作完成後，如果是在添加成員，無論成功失敗都關閉對話框
            if (isAddingMember && showAddMemberDialog) {
                showAddMemberDialog = false
            }
            isPerformingOperation = false
            isAddingMember = false
        }
    }

    // 使用 ToastManager 管理 Toast 顯示
    ToastManager(
        error = uiState.error,
        successMessage = uiState.successMessage,
        onErrorCleared = { viewModel.clearError() },
        onSuccessCleared = { 
            viewModel.clearSuccessMessage()
        }
    )
    
    // 特殊處理：刪除群組成功後返回
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            if (message == "群組刪除成功") {
                delay(1500) // 讓用戶看到成功訊息
                onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "群組詳情",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.arrow_back_24dp),
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                actions = {
                    // 編輯按鈕
                    if (groupDetails?.isAdmin == true) {
                        IconButton(onClick = { 
                            showEditGroupDialog = true
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.edit_24dp),
                                contentDescription = "編輯群組",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    // 更多操作菜單
                    if (groupDetails?.isAdmin == true) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.more_vert_24dp),
                                    contentDescription = "更多操作",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            "添加成員",
                                            style = MaterialTheme.typography.bodyLarge
                                        ) 
                                    },
                                    onClick = {
                                        showAddMemberDialog = true
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.person_24dp),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            "刪除群組",
                                            style = MaterialTheme.typography.bodyLarge
                                        ) 
                                    },
                                    onClick = {
                                        showDeleteConfirmDialog = true
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.delete_24dp),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "載入群組詳情中...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (uiState.error != null && !hasInitiallyLoaded) {
                ErrorState(
                    error = uiState.error ?: "未知錯誤",
                    onRetry = { viewModel.loadGroupDetails(groupId) },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (groupDetails != null) {
                // 群組頭像和基本信息
                GroupProfileHeader(
                    group = groupDetails!!.group,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
                
                // 群組詳細信息
                GroupInfoSection(
                    group = groupDetails!!.group,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                
                // 成員管理區域
                MemberManagementSection(
                    members = groupDetails!!.members.filter { member ->
                        currentUser?.let { user ->
                            member.userId != user.id && member.email != user.email
                        } ?: true
                    },
                    isCurrentUserAdmin = groupDetails!!.isAdmin,
                    onAddMember = { showAddMemberDialog = true },
                    onRemoveMember = { memberToRemove = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }

    // 對話框
    if (showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { showAddMemberDialog = false },
            onConfirm = { email ->
                isPerformingOperation = true
                isAddingMember = true
                viewModel.addMember(groupId, email, "member")
                // 不立即關閉對話框，等待操作完成後自動關閉
            },
            isLoading = uiState.isLoading
        )
    }

    if (showEditGroupDialog && groupDetails != null) {
        EditGroupDialog(
            group = groupDetails!!.group,
            onDismiss = { showEditGroupDialog = false },
            onConfirm = { name, description ->
                isPerformingOperation = true
                viewModel.updateGroup(groupId, name, description)
                showEditGroupDialog = false
            }
        )
    }

    if (showDeleteConfirmDialog) {
        DeleteGroupConfirmDialog(
            onDismiss = { showDeleteConfirmDialog = false },
            onConfirm = {
                isPerformingOperation = true
                viewModel.deleteGroup(groupId)
                showDeleteConfirmDialog = false
            }
        )
    }

    memberToRemove?.let { member ->
        RemoveMemberConfirmDialog(
            member = member,
            onDismiss = { memberToRemove = null },
            onConfirm = {
                isPerformingOperation = true
                viewModel.removeMember(groupId, member.userId)
                memberToRemove = null
            }
        )
    }
}


// 群組頭像和基本信息組件
@Composable
fun GroupProfileHeader(
    group: FamilyGroup,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "群組資訊",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = group.groupName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.person_24dp),
                        contentDescription = "群組頭像",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 群組描述
            if (!group.description.isNullOrBlank()) {
                Text(
                    text = group.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// 群組詳細信息組件
@Composable
fun GroupInfoSection(
    group: FamilyGroup,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "群組資料",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            
            InfoRow(
                label = "群組名稱",
                value = group.groupName,
                icon = painterResource(id = R.drawable.person_24dp)
            )
            
            InfoRow(
                label = "群組描述",
                value = group.description ?: "未設定",
                icon = painterResource(id = R.drawable.info_24dp)
            )
            
            InfoRow(
                label = "創建時間",
                value = formatDate(group.createdTime),
                icon = painterResource(id = R.drawable.date_range_24dp)
            )
            
            InfoRow(
                label = "創建者",
                value = group.creatorName ?: "未知",
                icon = painterResource(id = R.drawable.person_24dp)
            )
        }
    }
}

// 成員管理區域組件
@Composable
fun MemberManagementSection(
    members: List<FamilyGroupMember>,
    isCurrentUserAdmin: Boolean,
    onAddMember: () -> Unit,
    onRemoveMember: (FamilyGroupMember) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "成員 (${members.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (isCurrentUserAdmin) {
                    FilledTonalButton(
                        onClick = onAddMember,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            painterResource(id = R.drawable.add_24dp),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "添加成員",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 成員列表
            members.forEach { member ->
                MemberManagementCard(
                    member = member,
                    isCurrentUserAdmin = isCurrentUserAdmin,
                    onRemoveMember = onRemoveMember
                )
                if (member != members.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

// 成員管理卡片組件
@Composable
fun MemberManagementCard(
    member: FamilyGroupMember,
    isCurrentUserAdmin: Boolean,
    onRemoveMember: (FamilyGroupMember) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 成員頭像
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (member.role == "admin") {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (member.userImage != null) {
                    // 顯示用戶頭像
                    member.userImage?.let { userImageBytes ->
                        Image(
                            bitmap = BitmapFactory.decodeByteArray(
                                userImageBytes,
                                0,
                                userImageBytes.size
                            ).asImageBitmap(),
                            contentDescription = "用戶頭像",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    // 顯示默認圖標
                    Icon(
                        painter = painterResource(id = R.drawable.person_24dp),
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(12.dp),
                        tint = if (member.role == "admin") {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(20.dp))
            
            // 成員信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = member.userName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (member.role == "admin") {
                        AdminBadge()
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = member.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                member.joinedTime?.let { date ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "加入於 ${formatDate(date)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 移除按鈕（只有管理員可以看到，且不能移除其他管理員）
            if (isCurrentUserAdmin && member.role != "admin") {
                IconButton(
                    onClick = { onRemoveMember(member) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.close_24dp),
                        contentDescription = "移除成員",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// 信息行組件
@Composable
fun InfoRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.painter.Painter,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(18.dp)
                    .padding(7.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(100.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
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

@Composable
fun GroupInfoCard(group: FamilyGroup) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                        ),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
                    )
                )
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
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.person_24dp),
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .padding(12.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = group.groupName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        if (!group.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = group.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 詳細信息區域
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 創建者信息
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(24.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.person_24dp),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(4.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "創建者",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = group.creatorName ?: "未知",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // 創建時間信息
                        Column(horizontalAlignment = Alignment.End) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(24.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                ) {
                                    Icon(
                                        painterResource(id = R.drawable.info_24dp),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(4.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "創建時間",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = group.createdTime?.let { 
                                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(it) 
                                } ?: "未知",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MemberCard(
    member: FamilyGroupMember,
    isCurrentUserAdmin: Boolean,
    onRemoveMember: (FamilyGroupMember) -> Unit
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
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 用戶頭像
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (member.role == "admin") {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (member.userImage != null) {
                            // 顯示用戶頭像
                            member.userImage?.let { userImageBytes ->
                                Image(
                                    bitmap = BitmapFactory.decodeByteArray(
                                        userImageBytes,
                                        0,
                                        userImageBytes.size
                                    ).asImageBitmap(),
                                    contentDescription = "用戶頭像",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        } else {
                            // 顯示默認圖標
                            Icon(
                                painter = painterResource(id = R.drawable.person_24dp),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(28.dp)
                                    .padding(10.dp),
                                tint = if (member.role == "admin") {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = member.userName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (member.role == "admin") {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = if (member.role == "admin") R.drawable.settings_24dp else R.drawable.person_24dp),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (member.role == "admin") {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = when (member.role) {
                                            "admin" -> "管理員"
                                            else -> "成員"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = if (member.role == "admin") {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        }
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.mail_24dp),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = member.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        member.joinedTime?.let { date ->
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painterResource(id = R.drawable.info_24dp),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "加入於 ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // 移除按鈕（只有管理員可以看到，且不能移除其他管理員）
                if (isCurrentUserAdmin && member.role != "admin") {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                    ) {
                        IconButton(
                            onClick = { onRemoveMember(member) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.close_24dp),
                                contentDescription = "移除成員",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    isLoading: Boolean = false
) {
    var email by remember { mutableStateOf("") }
    var isEmailError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "添加成員",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            ) 
        },
        text = {
            Column {
                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        isEmailError = !android.util.Patterns.EMAIL_ADDRESS.matcher(it).matches()
                    },
                    label = { Text("電子郵件") },
                    isError = isEmailError && email.isNotEmpty(),
                    supportingText = if (isEmailError && email.isNotEmpty()) {
                        { Text("請輸入有效的電子郵件地址") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    if (email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        onConfirm(email.trim())
                    } else {
                        isEmailError = true
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text("添加中...")
                    }
                } else {
                    Text("添加")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("取消")
            }
        }
    )
}

@Composable
fun EditGroupDialog(
    group: FamilyGroup,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var groupName by remember { mutableStateOf(group.groupName) }
    var description by remember { mutableStateOf(group.description ?: "") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "編輯群組",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            ) 
        },
        text = {
            Column {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { 
                        groupName = it
                        isError = it.isBlank()
                    },
                    label = { Text("群組名稱") },
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("群組名稱不能為空") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("群組描述（選填）") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        confirmButton = {
            FilledTonalButton(
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
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("取消")
            }
        }
    )
}

@Composable
fun DeleteGroupConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "刪除群組",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            ) 
        },
        text = { 
            Text(
                "確定要刪除這個群組嗎？此操作無法撤銷。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            ) 
        },
        confirmButton = {
            FilledTonalButton(
                onClick = onConfirm,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("刪除")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("取消")
            }
        }
    )
}

@Composable
fun RemoveMemberConfirmDialog(
    member: FamilyGroupMember,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "移除成員",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            ) 
        },
        text = { 
            Text(
                "確定要將 ${member.userName} 從群組中移除嗎？",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            ) 
        },
        confirmButton = {
            FilledTonalButton(
                onClick = onConfirm,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("移除")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("取消")
            }
        }
    )
}
