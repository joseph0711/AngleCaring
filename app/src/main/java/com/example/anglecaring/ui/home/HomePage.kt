package com.example.anglecaring.ui.home

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.anglecaring.R
import com.example.anglecaring.ui.alarmhistory.AlarmHistoryViewModel
import com.example.anglecaring.ui.sensorhistory.SensorHistoryViewModel
import com.example.anglecaring.ui.sensorhistory.SensorHistoryState
import com.example.anglecaring.ui.theme.AngleCaringTheme
import com.example.anglecaring.ui.alarmhistory.AlarmHistoryState
import com.example.anglecaring.ui.monitoringstatus.MonitoringStatusViewModel
import com.example.anglecaring.ui.monitoringstatus.MonitoringStatusState
import com.example.anglecaring.ui.sensorstatus.SensorStatusViewModel
import com.example.anglecaring.ui.sensorstatus.SensorStatusState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    userName: String,
    userImage: ByteArray? = null,
    userId: String,
    onLogout: () -> Unit,
    onNavigateToSensorHistory: () -> Unit,
    onNavigateToAlarmHistory: () -> Unit,
    onNavigateToFamilyGroup: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToMonitoringStatus: () -> Unit,
    onNavigateToSensorStatus: () -> Unit,
    onNavigateToBedtimeSettings: () -> Unit,
    alarmHistoryViewModel: AlarmHistoryViewModel = viewModel(),
    sensorHistoryViewModel: SensorHistoryViewModel = viewModel(),
    monitoringStatusViewModel: MonitoringStatusViewModel = viewModel(),
    sensorStatusViewModel: SensorStatusViewModel = viewModel()
) {
    val alarmState by alarmHistoryViewModel.historyState.collectAsState()
    val sensorState by sensorHistoryViewModel.historyState.collectAsState()
    val monitoringStatusState by monitoringStatusViewModel.historyState.collectAsState()
    val sensorStatusState by sensorStatusViewModel.historyState.collectAsState()
    
    // 追蹤是否為第一次載入
    var isFirstLoad by remember { mutableStateOf(true) }
    
    // 儲存上次的狀態值
    var lastMonitoringStatus by remember { mutableStateOf("正常") }
    var lastSensorStatus by remember { mutableStateOf("正常") }
    
    // 載入資料
    LaunchedEffect(Unit) {
        alarmHistoryViewModel.loadAlarmHistory()
        sensorHistoryViewModel.loadSensorHistory()
        monitoringStatusViewModel.loadMonitoringStatus(userId.toInt())
        sensorStatusViewModel.loadSensorStatus(userId.toInt())
    }
    
    // 更新狀態值
    LaunchedEffect(monitoringStatusState) {
        when (val state = monitoringStatusState) {
            is MonitoringStatusState.Success -> {
                lastMonitoringStatus = state.summary.overallStatus
            }
            else -> { /* 不需要處理其他狀態 */ }
        }
    }
    
    LaunchedEffect(sensorStatusState) {
        when (val state = sensorStatusState) {
            is SensorStatusState.Success -> {
                lastSensorStatus = state.summary.overallStatus
            }
            else -> { /* 不需要處理其他狀態 */ }
        }
    }
    
    // 監聽狀態變化，當第一次載入完成時開始自動更新
    LaunchedEffect(monitoringStatusState, sensorStatusState) {
        if (isFirstLoad && 
            monitoringStatusState !is MonitoringStatusState.Loading && 
            sensorStatusState !is SensorStatusState.Loading) {
            isFirstLoad = false
            
            // 開始每五秒自動更新
            while (true) {
                delay(5000) // 等待5秒
                monitoringStatusViewModel.refreshData(userId.toInt())
                sensorStatusViewModel.refreshData(userId.toInt())
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("首頁", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            painter = painterResource(id = R.drawable.account_circle_24dp),
                            contentDescription = "個人資料",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            painter = painterResource(id = R.drawable.logout_24dp),
                            contentDescription = "登出",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // 用戶狀態摘要卡片
            item {
                StatusSummaryCard(
                    userName = userName,
                    userImage = userImage,
                    userId = userId,
                    alarmState = alarmState,
                    sensorState = sensorState
                )
            }
            
            // 狀態概覽
            item {
                StatusOverview(
                    onMonitoringStatusClick = onNavigateToMonitoringStatus,
                    onSensorStatusClick = onNavigateToSensorStatus,
                    monitoringStatusState = monitoringStatusState,
                    sensorStatusState = sensorStatusState,
                    isFirstLoad = isFirstLoad,
                    lastMonitoringStatus = lastMonitoringStatus,
                    lastSensorStatus = lastSensorStatus
                )
            }
            
            // 功能詳情標題
            item {
                Text(
                    text = "更多功能",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // 警報歷史紀錄卡片
            item {
                FunctionCardWithDrawable(
                    title = "警報歷史紀錄",
                    iconRes = R.drawable.warning_24dp,
                    onClick = onNavigateToAlarmHistory,
                    content = {}
                )
            }
            
            // 感應器紀錄卡片
            item {
                FunctionCardWithDrawable(
                    title = "感應器歷史紀錄",
                    iconRes = R.drawable.sensors_24dp,
                    onClick = onNavigateToSensorHistory,
                    content = {}
                )
            }
            
            // 家庭群組功能卡片
            item {
                FunctionCardWithDrawable(
                    title = "家庭群組",
                    iconRes = R.drawable.group_24dp,
                    onClick = onNavigateToFamilyGroup,
                    content = {}
                )
            }
            
            // 上下床時間設定功能卡片
            item {
                FunctionCardWithDrawable(
                    title = "上下床時間設定",
                    iconRes = R.drawable.bed_24dp,
                    onClick = onNavigateToBedtimeSettings,
                    content = {}
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun StatusSummaryCard(
    userName: String,
    userImage: ByteArray?,
    userId: String,
    alarmState: AlarmHistoryState,
    sensorState: SensorHistoryState
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "歡迎回來",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = userName,
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
                    if (userImage != null) {
                        // 顯示用戶頭像
                        Image(
                            bitmap = BitmapFactory.decodeByteArray(
                                userImage,
                                0,
                                userImage.size
                            ).asImageBitmap(),
                            contentDescription = "用戶頭像",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // 顯示默認圖標
                        Icon(
                            painter = painterResource(id = R.drawable.person_24dp),
                            contentDescription = "用戶頭像",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "${SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.TRADITIONAL_CHINESE).format(Date())}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}


@Composable
fun FunctionCardWithDrawable(
    title: String,
    iconRes: Int,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Icon(
                    painter = painterResource(id = R.drawable.arrow_forward_24dp),
                    contentDescription = "查看更多",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // 添加說明文字
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when (title) {
                    "警報歷史紀錄" -> "查看所有警報事件"
                    "感應器歷史紀錄" -> "查看感應器歷史記錄"
                    "家庭群組" -> "查看家庭群組成員或群組設定"
                    "上下床時間設定" -> "設定上床及下床時間"
                    else -> ""
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 64.dp)
            )
        }
    }
}

@Composable
fun FunctionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Icon(
                painter = painterResource(id = R.drawable.arrow_forward_24dp),
                contentDescription = "查看更多",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun StatusOverview(
    onMonitoringStatusClick: () -> Unit,
    onSensorStatusClick: () -> Unit,
    monitoringStatusState: MonitoringStatusState,
    sensorStatusState: SensorStatusState,
    isFirstLoad: Boolean,
    lastMonitoringStatus: String,
    lastSensorStatus: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 監控狀態卡片
        StatusCard(
            iconRes = R.drawable.person_24dp,
            value = when (monitoringStatusState) {
                is MonitoringStatusState.Loading -> if (isFirstLoad) "載入中..." else lastMonitoringStatus
                is MonitoringStatusState.Success -> monitoringStatusState.summary.overallStatus
                is MonitoringStatusState.Error -> "錯誤"
            },
            label = "被監控人員目前狀態",
            modifier = Modifier.weight(1f),
            color = when (monitoringStatusState) {
                is MonitoringStatusState.Loading -> if (isFirstLoad) Color(0xFF9E9E9E) else 
                    if (lastMonitoringStatus == "正常") Color(0xFF4CAF50) else Color(0xFFF44336)
                is MonitoringStatusState.Success -> if (monitoringStatusState.summary.overallStatus == "正常") 
                    Color(0xFF4CAF50) else Color(0xFFF44336)
                is MonitoringStatusState.Error -> Color(0xFFF44336)
            },
            onClick = onMonitoringStatusClick,
            isLoading = monitoringStatusState is MonitoringStatusState.Loading && isFirstLoad
        )
        
        // 感應器目前狀態卡片
        StatusCard(
            iconRes = R.drawable.sensors_24dp,
            value = when (sensorStatusState) {
                is SensorStatusState.Loading -> if (isFirstLoad) "載入中..." else lastSensorStatus
                is SensorStatusState.Success -> sensorStatusState.summary.overallStatus
                is SensorStatusState.Error -> "錯誤"
            },
            label = "感應器目前狀態",
            modifier = Modifier.weight(1f),
            color = when (sensorStatusState) {
                is SensorStatusState.Loading -> if (isFirstLoad) Color(0xFF9E9E9E) else 
                    if (lastSensorStatus == "正常") Color(0xFF4CAF50) else Color(0xFFF44336)
                is SensorStatusState.Success -> if (sensorStatusState.summary.overallStatus == "正常") 
                    Color(0xFF4CAF50) else Color(0xFFF44336)
                is SensorStatusState.Error -> Color(0xFFF44336)
            },
            onClick = onSensorStatusClick,
            isLoading = sensorStatusState is SensorStatusState.Loading && isFirstLoad
        )
    }
}

@Composable
fun StatusCard(
    iconRes: Int,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    isLoading: Boolean = false
) {
    Card(
        modifier = modifier
            .let { if (onClick != null) it.clickable { onClick() } else it },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = color,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomePagePreview() {
    AngleCaringTheme {
        HomePage(
            userName = "王小明",
            userImage = null,
            userId = "1",
            onLogout = {},
            onNavigateToSensorHistory = {},
            onNavigateToAlarmHistory = {},
            onNavigateToFamilyGroup = {},
            onNavigateToProfile = {},
            onNavigateToMonitoringStatus = {},
            onNavigateToSensorStatus = {},
            onNavigateToBedtimeSettings = {},
            alarmHistoryViewModel = viewModel(),
            sensorHistoryViewModel = viewModel(),
            monitoringStatusViewModel = viewModel(),
            sensorStatusViewModel = viewModel()
        )
    }
} 