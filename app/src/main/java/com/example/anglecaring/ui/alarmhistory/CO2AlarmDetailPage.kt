package com.example.anglecaring.ui.alarmhistory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.anglecaring.R
import com.example.anglecaring.data.model.AlarmEvent
import com.example.anglecaring.data.model.AlarmRiskLevel
import com.example.anglecaring.data.model.SensorReading
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.tooling.preview.Preview
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CO2AlarmDetailPage(
    alarm: AlarmEvent,
    onNavigateBack: () -> Unit,
    viewModel: CO2AlarmDetailViewModel = viewModel()
) {
    val detailState by viewModel.detailState.collectAsState()
    
    // 載入感測器讀數數據
    LaunchedEffect(alarm.alarmId) {
        viewModel.loadSensorReadings(alarm.alarmId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("二氧化碳警報詳情") },
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
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (detailState) {
                    is CO2AlarmDetailState.Loading -> {
                        // 顯示警報資訊，但感測器讀數載入中
                        CO2AlarmDetailContent(
                            alarm = alarm,
                            sensorReadings = emptyList(),
                            isLoadingSensorData = true
                        )
                    }
                    is CO2AlarmDetailState.Success -> {
                        val successState = detailState as CO2AlarmDetailState.Success
                        CO2AlarmDetailContent(
                            alarm = alarm,
                            sensorReadings = successState.sensorReadings,
                            isLoadingSensorData = false
                        )
                    }
                    is CO2AlarmDetailState.Error -> {
                        // 顯示警報資訊，但感測器數據載入失敗
                        val errorState = detailState as CO2AlarmDetailState.Error
                        CO2AlarmDetailContent(
                            alarm = alarm,
                            sensorReadings = emptyList(),
                            isLoadingSensorData = false,
                            sensorDataError = errorState.message
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CO2AlarmDetailContent(
    alarm: AlarmEvent,
    sensorReadings: List<SensorReading>,
    isLoadingSensorData: Boolean = false,
    sensorDataError: String? = null
) {
    val scrollState = rememberScrollState()

    // 根據風險等級計算 CO2 標準值
    val standardValueForChart = when (alarm.riskLevel) {
        AlarmRiskLevel.LOW -> 700f    // 警告
        AlarmRiskLevel.MEDIUM -> 1000f   // 危險
        AlarmRiskLevel.HIGH -> 2500f    // 嚴重
        else -> alarm.standardValue ?: 1000f  // 默認值或從數據庫獲取的值
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // 警報標題與風險等級
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 風險等級指示器
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = when (alarm.riskLevel ?: "") {
                                    AlarmRiskLevel.HIGH -> Color.Red
                                    AlarmRiskLevel.MEDIUM -> Color(0xFFFFA000)
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                shape = CircleShape
                            )
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // 風險等級標籤
                    val riskText = when (alarm.riskLevel ?: "") {
                        AlarmRiskLevel.HIGH -> "嚴重"
                        AlarmRiskLevel.MEDIUM -> "危險"
                        AlarmRiskLevel.LOW -> "警告"
                        else -> "未知"
                    }
                    
                    Text(
                        text = riskText,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (alarm.riskLevel ?: "") {
                            AlarmRiskLevel.HIGH -> Color.Red
                            AlarmRiskLevel.MEDIUM -> Color(0xFFFFA000)
                            else -> MaterialTheme.colorScheme.primary
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // 時間標示
                    val formattedTime = alarm.alarmTime?.let { time ->
                        time.replace("T", " ")
                            .replace("Z", "")
                            .replace(Regex("\\.\\d+"), "")
                    } ?: "未知時間"
                    
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 警報標題
                Text(
                    text = alarm.alarmLabel ?: "未命名警報",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 警報描述
                Text(
                    text = getCO2AlarmDescription(alarm),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 感應器資訊卡片
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 標題
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.info_24dp),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "感應器資訊",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                
                // 感應器名稱
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "感應器類型:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(100.dp)
                    )
                    
                    Text(
                        text = alarm.deviceName?.let { mapSensorNameToChinese(it) } ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // 位置
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "位置:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(100.dp)
                    )
                    
                    Text(
                        text = alarm.deviceLocation?.let { mapLocationToChinese(it) } ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // 當前濃度
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "濃度:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(100.dp)
                    )
                    
                    Text(
                        text = "${alarm.value?.let { String.format("%.1f", it) } ?: "未知"} ppm",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = " (${alarm.riskLevel}標準: ${String.format("%.1f", standardValueForChart)} ppm)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // 濃度變化趨勢圖
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 標題
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.info_24dp),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "濃度變化趨勢",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))
                
                // 圖表
                when {
                    isLoadingSensorData -> {
                        // 載入中狀態
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "載入趨勢圖數據中...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    sensorDataError != null -> {
                        // 錯誤狀態
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "載入趨勢圖失敗",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = sensorDataError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    sensorReadings.isNotEmpty() -> {
                        // 有數據，顯示圖表 - 使用 COChart 但傳入 CO2 設定
                        COChart(
                            readings = sensorReadings,
                            standardValue = standardValueForChart,
                            alarmValue = alarm.value,
                            deviceType = alarm.deviceName ?: "co2",
                            alarmTime = alarm.alarmTime
                        )
                    }
                    else -> {
                        // 無數據狀態
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暫無歷史濃度數據",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

            }
        }
        
        // 安全建議卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (alarm.riskLevel ?: "") {
                    AlarmRiskLevel.HIGH -> Color.Red.copy(alpha = 0.05f)
                    AlarmRiskLevel.MEDIUM -> Color(0xFFFFA000).copy(alpha = 0.05f)
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 標題
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.info_24dp),
                        contentDescription = null,
                        tint = when (alarm.riskLevel ?: "") {
                            AlarmRiskLevel.HIGH -> Color.Red
                            AlarmRiskLevel.MEDIUM -> Color(0xFFFFA000)
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "安全建議",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when (alarm.riskLevel ?: "") {
                            AlarmRiskLevel.HIGH -> Color.Red
                            AlarmRiskLevel.MEDIUM -> Color(0xFFFFA000)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                }
                
                HorizontalDivider(
                    modifier = Modifier.padding(bottom = 12.dp),
                    color = when (alarm.riskLevel ?: "") {
                        AlarmRiskLevel.HIGH -> Color.Red.copy(alpha = 0.2f)
                        AlarmRiskLevel.MEDIUM -> Color(0xFFFFA000).copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    }
                )
                
                // 安全建議
                val recommendations = getCO2SafetyRecommendations(alarm.riskLevel ?: AlarmRiskLevel.LOW)
                recommendations.forEach { recommendation ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = when (alarm.riskLevel ?: "") {
                                        AlarmRiskLevel.HIGH -> Color.Red
                                        AlarmRiskLevel.MEDIUM -> Color(0xFFFFA000)
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    shape = CircleShape
                                )
                                .align(Alignment.CenterVertically)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = recommendation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        
        // 底部空間
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// 根據警報風險等級提供 CO2 安全建議
private fun getCO2SafetyRecommendations(riskLevel: String): List<String> {
    return when (riskLevel) {
        AlarmRiskLevel.HIGH -> listOf(
            "立即開啟所有窗戶進行通風",
            "檢查並關閉可能的 CO2 來源設備",
            "如出現呼吸困難、頭暈等症狀請立即就醫",
            "暫時離開該區域至空氣流通處",
            "檢查空調系統是否正常運作",
            "考慮增設空氣清淨設備"
        )
        AlarmRiskLevel.MEDIUM -> listOf(
            "增加室內通風",
            "檢查空調系統設定",
            "監測濃度變化",
            "減少室內人員密度",
            "注意呼吸狀況"
        )
        else -> listOf(
            "保持適度通風",
            "定期檢查空調系統",
            "監測室內空氣品質",
            "注意室內人員數量"
        )
    }
}

// 生成 CO2 警報描述
private fun getCO2AlarmDescription(alarm: AlarmEvent): String {
    return when (alarm.riskLevel) {
        AlarmRiskLevel.HIGH -> "二氧化碳濃度已達嚴重等級"
        AlarmRiskLevel.MEDIUM -> "二氧化碳濃度已達危險等級"
        else -> "二氧化碳濃度已達警告等級"
    }
}

// 感應器名稱對應中文顯示的函數
private fun mapSensorNameToChinese(sensorName: String): String {
    return when (sensorName.lowercase()) {
        "co" -> "一氧化碳"
        "co2" -> "二氧化碳"
        "ir" -> "紅外線感應器"
        else -> sensorName // 若沒有對應翻譯，保留原名
    }
}

// 位置名稱對應中文顯示的函數
private fun mapLocationToChinese(location: String): String {
    return when (location.lowercase()) {
        "bedroom" -> "臥室"
        "living room" -> "客廳"
        "kitchen" -> "廚房"
        "bathroom" -> "浴室"
        else -> location // 若沒有對應翻譯，保留原名
    }
}

@Preview(showBackground = true)
@Composable
fun CO2AlarmDetailPagePreview() {
    MaterialTheme {
        // 創建模擬的感測器讀數數據，包含一些變化趨勢
        val mockSensorReadings = List(24) { index ->
            val baseTime = System.currentTimeMillis() - (24 - index) * 3600000L // 24小時前開始
            val baseValue = 800f + (index * 50f) + kotlin.random.Random.nextFloat() * 200f
            val finalValue = if (index > 18) baseValue + 2000f else baseValue // 最後幾個小時濃度升高
            
            SensorReading(
                sensorReadingId = index,
                deviceId = "CO2-SENSOR-001",
                readingTime = Date(baseTime),
                booleanValue = null,
                numericValue = finalValue,
                createdAt = Date(),
                isAlarmPoint = false
            )
        }
        
        CO2AlarmDetailContent(
            alarm = AlarmEvent(
                alarmId = 1,
                userId = 1,
                alarmTime = "2024-03-20 10:00:00",
                alarmLabel = "CO2 level exceeded",
                riskLevel = AlarmRiskLevel.HIGH,
                deviceId = "CO2-SENSOR-001",
                deviceName = "co2",
                deviceLocation = "living room",
                value = 5200f,
                standardValue = 1000f
            ),
            sensorReadings = mockSensorReadings,
            isLoadingSensorData = false
        )
    }
} 