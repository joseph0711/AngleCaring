package com.example.anglecaring.ui.sensorstatus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.anglecaring.R
import com.example.anglecaring.data.model.*
import com.example.anglecaring.ui.components.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorStatusPage(
    userId: Int,
    onNavigateBack: () -> Unit,
    sensorStatusViewModel: SensorStatusViewModel = viewModel()
) {
    val statusState by sensorStatusViewModel.historyState.collectAsState()
    
    // 載入資料
    LaunchedEffect(Unit) {
        sensorStatusViewModel.loadSensorStatus(userId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("感應器狀態", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.arrow_back_24dp),
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { sensorStatusViewModel.refreshData(userId) }) {
                        Icon(
                            painter = painterResource(id = R.drawable.refresh_24dp),
                            contentDescription = "重新整理"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
            tonalElevation = 1.dp
        ) {
            when (statusState) {
                is SensorStatusState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "載入中...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                is SensorStatusState.Success -> {
                    val summary = (statusState as SensorStatusState.Success).summary
                    
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
                        
                        // 智能狀態卡片（合併目前狀態和錯誤訊息）
                        item {
                            SmartSensorStatusCard(
                                summary = summary,
                                errorMessages = summary.errorMessages
                            )
                        }
                        
                        // 設備狀態列表
                        item {
                            DeviceStatusListCard(
                                deviceStatuses = summary.deviceStatuses,
                                normalDevices = summary.normalDevices,
                                abnormalDevices = summary.abnormalDevices
                            )
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
                
                is SensorStatusState.Error -> {
                    val error = (statusState as SensorStatusState.Error).message
                    
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.warning_24dp),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(64.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "載入資料發生錯誤",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Button(
                                onClick = { sensorStatusViewModel.refreshData(userId) }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.refresh_24dp),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("重新整理")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmartSensorStatusCard(
    summary: SensorStatusSummary,
    errorMessages: List<String>
) {
    val hasErrors = errorMessages.isNotEmpty()
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (hasErrors) {
                Color(0xFFFFEBEE) // 淺粉色背景，與截圖一致
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        ),
        shape = RoundedCornerShape(20.dp) // 增加圓角半徑以匹配截圖
    ) {
        Column(
            modifier = Modifier.padding(20.dp) // 增加內邊距
        ) {
            // 標題區域
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (hasErrors) R.drawable.warning_24dp else R.drawable.check_24dp
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (hasErrors) Color(0xFFD32F2F) else Color(0xFF4CAF50) // 調整紅色以匹配截圖
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = "目前狀態",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (hasErrors) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onPrimaryContainer // 調整文字顏色
                    )
                }
                
                // 狀態指示器
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (summary.overallStatus == "正常" && !hasErrors) 
                                Color(0xFF4CAF50) 
                            else 
                                Color(0xFFD32F2F) // 調整紅色以匹配截圖
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp)) // 減少間距
            
            if (hasErrors) {
                // 有錯誤時顯示錯誤資訊
                Column {
                    // 整體狀態
                    Text(
                        text = summary.overallStatus,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F) // 調整紅色以匹配截圖
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 錯誤訊息標題
                    Text(
                        text = "錯誤訊息:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFD32F2F) // 調整顏色以匹配截圖
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 錯誤訊息列表
                    errorMessages.forEach { message ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD32F2F)) // 調整紅色以匹配截圖
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFD32F2F), // 調整紅色以匹配截圖
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        if (message != errorMessages.last()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            } else {
                // 無錯誤時顯示正常狀態
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = summary.overallStatus,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "所有感應器運作正常",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp)) // 減少間距
            
            // 最後更新時間（統一顯示）
            Text(
                text = "最後更新: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(summary.lastUpdated)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f), // 調整透明度
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}



@Composable
fun DeviceStatusListCard(
    deviceStatuses: List<DeviceStatus>,
    normalDevices: List<DeviceStatus>,
    abnormalDevices: List<DeviceStatus>
) {
    val typeOrder: (String) -> Int = {
        when (it.lowercase()) {
            "ir" -> 0
            "co" -> 1
            "co2" -> 2
            else -> 99
        }
    }
    val sortedDevices = deviceStatuses.sortedBy { typeOrder(it.deviceType) }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.sensors_24dp),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "狀態詳情",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            sortedDevices.forEach { device ->
                DeviceStatusItem(device = device)
                
                if (device != sortedDevices.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun DeviceStatusItem(device: DeviceStatus) {
    val isAbnormal = device.isAbnormal
    val statusColor = if (isAbnormal) Color(0xFFF44336) else Color(0xFF4CAF50)
    val statusText = if (isAbnormal) "異常" else "正常"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 設備類型圖標
                Icon(
                    painter = painterResource(id = getDeviceTypeIcon(device.deviceType)),
                    contentDescription = getDeviceTypeDisplay(device.deviceType),
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // 設備類型標籤
                Text(
                    text = getDeviceTypeDisplay(device.deviceType),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // 狀態標籤
                Box(
                    modifier = Modifier
                        .background(
                            statusColor.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 位置
            Text(
                text = "位置: ${getLocationDisplay(device.location)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 最後在線時間
            device.lastOnlineTime?.let { lastOnlineTime ->
                Text(
                    text = "最後在線: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(lastOnlineTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 最後讀數時間
            device.lastReadingTime?.let { lastReadingTime ->
                Text(
                    text = "最後讀數: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(lastReadingTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Helper functions
private fun getDeviceTypeDisplay(type: String): String {
    return when (type.lowercase()) {
        "co2" -> "二氧化碳感應器"
        "co" -> "一氧化碳感應器"
        "ir" -> "紅外線感應器"
        "temperature" -> "溫度感應器"
        else -> type
    }
}

private fun getLocationDisplay(location: String): String {
    return when (location.lowercase()) {
        "bedroom" -> "臥室"
        "kitchen" -> "廚房"
        "livingroom" -> "客廳"
        "bathroom" -> "浴室"
        else -> location
    }
}

@Composable
private fun getDeviceTypeColor(type: String): Color {
    return when (type.lowercase()) {
        "co2" -> Color(0xFF388E3C) // Green
        "co" -> Color(0xFFE53935) // Red
        "ir" -> Color(0xFF1976D2) // Blue
        "temperature" -> Color(0xFFFF9800) // Orange
        else -> MaterialTheme.colorScheme.primary
    }
}

private fun getDeviceTypeIcon(type: String): Int {
    return when (type.lowercase()) {
        "co2" -> R.drawable.co2_24dp
        "co" -> R.drawable.detector_co_24dp
        "ir" -> R.drawable.infrared_24dp
        "temperature" -> R.drawable.thermometer_24dp
        else -> R.drawable.sensors_24dp
    }
}
