package com.example.anglecaring.ui.monitoringstatus

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
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
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.graphics.toColorInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringStatusPage(
    userId: Int,
    onNavigateBack: () -> Unit,
    monitoringStatusViewModel: MonitoringStatusViewModel = viewModel()
) {
    val statusState by monitoringStatusViewModel.historyState.collectAsState()
    
    // 載入資料
    LaunchedEffect(Unit) {
        monitoringStatusViewModel.loadMonitoringStatus(userId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("被監控人員目前狀態", fontWeight = FontWeight.Bold) },
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
                    IconButton(onClick = { monitoringStatusViewModel.refreshData(userId) }) {
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
                is MonitoringStatusState.Loading -> {
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
                
                is MonitoringStatusState.Success -> {
                    val summary = (statusState as MonitoringStatusState.Success).summary
                    
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
                        
                        // 智能狀態卡片（合併目前狀態和警報資訊）
                        item {
                            SmartStatusCard(
                                summary = summary,
                                alarm = summary.latestAlarm,
                                alarmLevel = summary.alarmLevel
                            )
                        }
                        
                        // 感應器最新資訊
                        item {
                            LatestSensorReadingsCard(
                                readings = summary.latestSensorReadings,
                                deviceStatuses = summary.deviceStatuses
                            )
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
                
                is MonitoringStatusState.Error -> {
                    val error = (statusState as MonitoringStatusState.Error).message
                    
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
                                onClick = { monitoringStatusViewModel.refreshData(userId) }
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
fun SmartStatusCard(
    summary: MonitoringStatusSummary,
    alarm: Alarm?,
    alarmLevel: AlarmLevel
) {
    val hasAlarm = alarm != null
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (hasAlarm) {
                Color(0xFFFFEBEE) // 淺粉色背景，與 SensorStatusPage 一致
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        ),
        shape = RoundedCornerShape(20.dp) // 增加圓角半徑以匹配 SensorStatusPage
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
                            id = if (hasAlarm) R.drawable.warning_24dp else R.drawable.check_24dp
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (hasAlarm) {
                            // 根據警報的 riskLevel 字串判斷顏色（支援中英文）
                            when (alarm.riskLevel?.lowercase()) {
                                "warning", "警告" -> Color(0xFFFF9800) // 橙色
                                "severe", "嚴重" -> Color(0xFFFF5722) // 深橙色
                                "danger", "危險" -> Color(0xFFD32F2F) // 調整紅色以匹配 SensorStatusPage
                                else -> {
                                    println("DEBUG: Unknown riskLevel: '${alarm.riskLevel}'")
                                    Color(0xFFD32F2F) // 預設紅色
                                }
                            }
                        } else {
                            Color(0xFF4CAF50)
                        }
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = if (hasAlarm) "警報狀態" else "目前狀態",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (hasAlarm) {
                            // 根據警報等級調整文字顏色
                            when (alarm.riskLevel?.lowercase()) {
                                "warning", "警告" -> Color(0xFFFF9800)
                                "severe", "嚴重" -> Color(0xFFFF5722)
                                "danger", "危險" -> Color(0xFFD32F2F)
                                else -> Color(0xFFD32F2F)
                            }
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
                
                // 狀態指示器
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (hasAlarm) {
                                // 根據警報的 riskLevel 字串判斷顏色（支援中英文）
                                when (alarm.riskLevel?.lowercase()) {
                                    "warning", "警告" -> Color(0xFFFF9800) // 橙色
                                    "severe", "嚴重" -> Color(0xFFFF5722) // 深橙色
                                    "danger", "危險" -> Color(0xFFD32F2F) // 調整紅色以匹配 SensorStatusPage
                                    else -> Color(0xFFD32F2F) // 預設紅色
                                }
                            } else {
                                Color(0xFF4CAF50)
                            }
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp)) // 減少間距
            
            if (hasAlarm) {
                // 調試日誌：檢查警報的 riskLevel 值
                println("DEBUG: Alarm riskLevel = '${alarm.riskLevel}'")
                
                // 有警報時顯示警報資訊
                Column {
                    // 警報等級
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "等級: ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Box(
                            modifier = Modifier
                            .background(
                                when (alarm.riskLevel?.lowercase()) {
                                    "warning", "警告" -> Color(0xFFFF9800).copy(alpha = 0.2f)
                                    "severe", "嚴重" -> Color(0xFFFF5722).copy(alpha = 0.2f)
                                    "danger", "危險" -> Color(0xFFD32F2F).copy(alpha = 0.2f) // 調整紅色
                                    else -> Color(0xFFD32F2F).copy(alpha = 0.2f) // 調整紅色
                                },
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = when (alarm.riskLevel?.lowercase()) {
                                    "warning", "警告" -> "警告"
                                    "severe", "嚴重" -> "嚴重"
                                    "danger", "危險" -> "危險"
                                    else -> "未知"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = when (alarm.riskLevel?.lowercase()) {
                                    "warning", "警告" -> Color(0xFFFF9800)
                                    "severe", "嚴重" -> Color(0xFFFF5722)
                                    "danger", "危險" -> Color(0xFFD32F2F) // 調整紅色
                                    else -> Color(0xFFD32F2F) // 調整紅色
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 警報內容
                    Text(
                        text = "警報內容: ${alarm.alarmLabel ?: "無"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 警報時間
                    Text(
                        text = "警報時間: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(alarm.alarmTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            } else {
                // 無警報時顯示正常狀態
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "正常",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "最近1小時內無警報記錄",
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
fun OverallStatusCard(summary: MonitoringStatusSummary) {
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
                Text(
                    text = "目前狀態",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                // 狀態指示器
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (summary.overallStatus == "正常") 
                                Color(0xFF4CAF50) 
                            else 
                                Color(0xFFF44336)
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = summary.overallStatus,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (summary.overallStatus == "正常") 
                    Color(0xFF4CAF50) 
                else 
                    Color(0xFFF44336)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "最後更新: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(summary.lastUpdated)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun LatestAlarmCard(
    alarm: Alarm?,
    alarmLevel: AlarmLevel
) {
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
                    painter = painterResource(id = R.drawable.warning_24dp),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "最新警報",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (alarm != null) {
                // 警報等級
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "等級: ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .background(
                                Color(alarmLevel.color.toColorInt()).copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = alarmLevel.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(alarmLevel.color.toColorInt())
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 警報標籤
                Text(
                    text = "警報內容: ${alarm.alarmLabel ?: "無"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 警報時間
                Text(
                    text = "警報時間: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(alarm.alarmTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // 顯示無警報狀態
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.check_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "目前無警報",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4CAF50)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "最近1小時內無警報記錄",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun LatestSensorReadingsCard(
    readings: List<SensorReading>,
    deviceStatuses: List<DeviceStatus>
) {
    val typeOrder: (String) -> Int = {
        when (it.lowercase()) {
            "ir" -> 0
            "co" -> 1
            "co2" -> 2
            else -> 99
        }
    }
    // 依據關聯到的 deviceType 排序 readings：IR → CO → CO2
    val sortedReadings = readings.sortedBy { reading ->
        val type = deviceStatuses.find { it.deviceId == reading.deviceId }?.deviceType ?: ""
        typeOrder(type)
    }
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
                    text = "感應器最新資訊",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (sortedReadings.isNotEmpty()) {
                sortedReadings.forEachIndexed { index, reading ->
                    // 調試日誌
                    println("DEBUG: Reading deviceId: '${reading.deviceId}'")
                    println("DEBUG: Available deviceStatuses: ${deviceStatuses.map { "'${it.deviceId}'" }}")
                    
                    val deviceStatus = deviceStatuses.find { it.deviceId == reading.deviceId }
                    println("DEBUG: Found deviceStatus: ${deviceStatus != null}")
                    
                    val deviceType = deviceStatus?.deviceType ?: "unknown"
                    val location = deviceStatus?.location ?: "unknown"
                    
                    println("DEBUG: Final deviceType: '$deviceType', location: '$location'")
                    
                    SensorReadingItem(
                        deviceType = deviceType,
                        location = location,
                        readingTime = reading.readingTime?.let { 
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(it) 
                        } ?: "未知時間",
                        booleanValue = reading.booleanValue,
                        numericValue = reading.numericValue
                    )
                    
                    // 在每個感應器項目之間添加間距，除了最後一個
                    if (index < sortedReadings.size - 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            } else {
                Text(
                    text = "目前無感應器數據",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SensorReadingItem(
    deviceType: String,
    location: String,
    readingTime: String,
    booleanValue: Boolean?,
    numericValue: Float?
) {
    // 判斷狀態和對應的顏色
    val statusColor = if (numericValue != null) {
        getReadingValueColor(deviceType, numericValue)
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    val statusText = if (numericValue != null) {
        getReadingStatusText(deviceType, numericValue)
    } else {
        "正常"
    }
    
    val isAbnormal = statusText != "正常"
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // 添加垂直內邊距
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAbnormal) {
                statusColor.copy(alpha = 0.1f) // 異常狀態使用對應顏色的淺色背景
            } else {
                MaterialTheme.colorScheme.surfaceVariant // 正常狀態使用預設背景
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // 無陰影
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 感應器類型圖標
                Icon(
                    painter = painterResource(id = getSensorTypeIcon(deviceType)),
                    contentDescription = getDeviceTypeDisplay(deviceType),
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // 感應器類型標籤
                Text(
                    text = getDeviceTypeDisplay(deviceType),
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
            
            // 位置資訊
            Text(
                text = "位置: ${getLocationDisplay(location)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 讀數時間
            Text(
                text = "讀數時間: $readingTime",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 感應器讀數
            Text(
                text = "數值: ${getReadingValueText(deviceType, numericValue ?: 0f, booleanValue)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Helper functions
private fun getDeviceTypeDisplay(type: String): String {
    return when (type.lowercase()) {
        "co2" -> "二氧化碳感應器"
        "co" -> "一氧化碳感應器"
        "ir" -> "紅外線感應器"
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
private fun getSensorTypeColor(type: String): Color {
    return when (type.lowercase()) {
        "co2" -> Color(0xFF388E3C) // Green
        "co" -> Color(0xFFE53935) // Red
        "ir" -> Color(0xFF1976D2) // Blue
        else -> MaterialTheme.colorScheme.primary
    }
}

private fun getSensorTypeIcon(type: String): Int {
    return when (type.lowercase()) {
        "co2" -> R.drawable.co2_24dp
        "co" -> R.drawable.detector_co_24dp
        "ir" -> R.drawable.infrared_24dp
        else -> R.drawable.sensors_24dp
    }
}

private fun getReadingValueText(type: String, value: Float, booleanValue: Boolean? = null): String {
    return when (type.lowercase()) {
        "co2" -> "${"%.1f".format(value)} ppm"
        "co" -> "${"%.1f".format(value)} ppm"
        "ir" -> if (booleanValue != null) {
                   if (booleanValue) "偵測到動靜" else "無動靜"
                } else {
                   "${"%.1f".format(value)}"
                }
        else -> "${"%.1f".format(value)}"
    }
}

// 根據感應器類型和數值判斷顏色
@Composable
private fun getReadingValueColor(type: String, value: Float): Color {
    return when (type.lowercase()) {
        "co" -> when {
            value <= COThresholds.NORMAL_MAX -> MaterialTheme.colorScheme.onSurface // 預設顏色 - 正常
            value <= COThresholds.WARNING_MAX -> Color(0xFFFF9800) // 橙色 - 警告
            value <= COThresholds.DANGER_MAX -> Color(0xFFFF5722) // 深橙色 - 危險
            else -> Color(0xFFF44336) // 紅色 - 嚴重
        }
        "co2" -> when {
            value <= CO2Thresholds.NORMAL_MAX -> MaterialTheme.colorScheme.onSurface // 預設顏色 - 正常
            value <= CO2Thresholds.WARNING_MAX -> Color(0xFFFF9800) // 橙色 - 警告
            value <= CO2Thresholds.DANGER_MAX -> Color(0xFFFF5722) // 深橙色 - 危險
            else -> Color(0xFFF44336) // 紅色 - 嚴重
        }
        else -> MaterialTheme.colorScheme.onSurface
    }
}

// 根據感應器類型和數值判斷狀態文字
private fun getReadingStatusText(type: String, value: Float): String {
    return when (type.lowercase()) {
        "co" -> when {
            value <= COThresholds.NORMAL_MAX -> "正常"
            value <= COThresholds.WARNING_MAX -> "警告"
            value <= COThresholds.DANGER_MAX -> "危險"
            else -> "嚴重"
        }
        "co2" -> when {
            value <= CO2Thresholds.NORMAL_MAX -> "正常"
            value <= CO2Thresholds.WARNING_MAX -> "警告"
            value <= CO2Thresholds.DANGER_MAX -> "危險"
            else -> "嚴重"
        }
        else -> ""
    }
}
