package com.example.anglecaring.ui.alarmhistory

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import com.example.anglecaring.data.model.SensorReading
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.point
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.vicoTheme
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.Position
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import java.text.SimpleDateFormat
import java.util.Locale

private val LegendLabelKey = ExtraStore.Key<Set<String>>()

@Composable
fun COChart(
    readings: List<SensorReading>,
    standardValue: Float = 9.4f,
    alarmValue: Float? = null,
    deviceType: String = "co",
    alarmTime: String? = null
) {
    var showDebugInfo by remember { mutableStateOf(false) }

    // 如果沒有數據，顯示佔位符
    if (readings.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📊",
                    fontSize = 48.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "暫無歷史濃度數據",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "當有感測器數據時，將顯示濃度變化趨勢圖",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        return
    }

    // 智能數據處理：確保包含警報時間點
    val processedReadings = remember(readings, alarmTime) {
        if (alarmTime != null && readings.isNotEmpty()) {
            // 解析警報時間
            Log.d("COChart", "嘗試解析警報時間: '$alarmTime'")

            // 清理時間格式，移除 T、Z 和毫秒部分
            val cleanedTime = alarmTime
                .replace("T", " ")
                .replace("Z", "")
                .replace(Regex("\\.\\d+"), "")

            Log.d("COChart", "清理後的時間格式: '$cleanedTime'")

            // 嘗試多種時間格式
            val alarmTimeDate = tryParseAlarmTime(cleanedTime, alarmTime)

            if (alarmTimeDate != null) {
                // 找到最接近警報時間的數據點
                val sortedReadings = readings.sortedBy { it.readingTime }

                // 增加除錯日誌：顯示所有讀數時間與警報時間的比較
                Log.d("COChart", "警報時間: $alarmTimeDate (${alarmTimeDate.time})")
                sortedReadings.forEachIndexed { index, reading ->
                    val timeDiff = reading.readingTime?.let {
                        Math.abs(it.time - alarmTimeDate.time)
                    } ?: -1
                    Log.d("COChart", "讀數 $index: 時間=${reading.readingTime}, 時間戳=${reading.readingTime?.time}, 與警報時間差=${timeDiff}ms (${timeDiff/1000.0}秒)")
                }

                // 找到最接近警報時間的數據點
                val alarmIndex = sortedReadings.mapIndexed { index, reading ->
                    val timeDiff = reading.readingTime?.let { readingTime ->
                        Math.abs(readingTime.time - alarmTimeDate.time)
                    } ?: Long.MAX_VALUE
                    Pair(index, timeDiff)
                }.minByOrNull { it.second }?.first ?: -1

                if (alarmIndex >= 0) {
                    // 從警報點往前取4筆 + 警報點本身 = 5筆
                    Log.d("COChart", "找到警報時間點，索引: $alarmIndex，選取警報點前五筆數據（包括警報本身）")
                    val startIndex = maxOf(0, alarmIndex - 4)
                    val endIndex = alarmIndex
                    val selectedReadings = sortedReadings.subList(startIndex, endIndex + 1)

                    Log.d("COChart", "選中的數據範圍: $startIndex 到 $endIndex，共 ${selectedReadings.size} 筆數據")
                    selectedReadings.forEachIndexed { index, reading ->
                        Log.d("COChart", "選中數據 $index: 時間=${reading.readingTime}, 數值=${reading.numericValue}")
                    }

                    // 確保只有最接近警報時間的數據點被標記為警報點
                    selectedReadings.mapIndexed { index, reading ->
                        val isExactAlarmPoint = (startIndex + index) == alarmIndex
                        Log.d("COChart", "數據 $index: 原始索引=${startIndex + index}, 警報索引=$alarmIndex, 是否為警報點=$isExactAlarmPoint")
                        reading.copy(isAlarmPoint = isExactAlarmPoint)
                    }
                } else {
                    // 找不到警報時間點，取最新的5筆
                    Log.w("COChart", "找不到警報時間點 (在60秒內的匹配)，取最新的5筆數據")
                    Log.w("COChart", "總共有 ${sortedReadings.size} 筆數據，這可能是時區問題")
                    sortedReadings.takeLast(5)
                }
            } else {
                // 無法解析警報時間，取最新的5筆
                Log.d("COChart", "無法解析警報時間，取最新的5筆數據")
                readings.sortedBy { it.readingTime }.takeLast(5)
            }
        } else {
            // 沒有警報時間，取最新的5筆
            Log.d("COChart", "沒有警報時間，取最新的5筆數據")
            readings.sortedBy { it.readingTime }.takeLast(5)
        }
    }

    // 創建圖表數據
    val chartData = remember(processedReadings) {
        val sensorData = mutableMapOf<Int, Number>()
        val alarmData = mutableMapOf<Int, Number>()
        val standardData = mutableMapOf<Int, Number>()
        
        processedReadings.forEachIndexed { index, reading ->
            val value = reading.numericValue ?: 0f
            sensorData[index] = value
            standardData[index] = standardValue
            
            if (reading.isAlarmPoint) {
                alarmData[index] = value
            }
        }
        
        val dataMap = mutableMapOf<String, Map<Int, Number>>()
        dataMap["感測器數據"] = sensorData
        dataMap["標準值"] = standardData
        if (alarmData.isNotEmpty()) {
            dataMap["警報點"] = alarmData
        }
        dataMap
    }

    // 時間格式化器
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {

        // Debug信息（摺疊式）
        if (showDebugInfo) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "🔍 詳細資訊",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "📊 資料筆數: ${readings.size} (顯示${processedReadings.size}筆)",
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (alarmTime != null) {
                        Text(
                            text = "🚨 警報時間: $alarmTime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (processedReadings.isNotEmpty()) {
                        val firstTime = processedReadings.first().readingTime
                        val lastTime = processedReadings.last().readingTime
                        Text(
                            text = "⏰ 時間範圍: ${firstTime?.let { timeFormatter.format(it) }} ~ ${lastTime?.let { timeFormatter.format(it) }}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Text(
                        text = "⚠️ 標準值: ${standardValue}ppm${alarmValue?.let { ", 警報值: ${String.format("%.1f", it)}ppm" } ?: ""}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // 圖例
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 感測器數據線 - 灰色
                LegendItem(
                    color = Color(0xFF666666),
                    label = "感測器數據",
                    pointStyle = true,
                    modifier = Modifier.weight(1f)
                )

                // 標準值線 - 橘色
                 LegendItem(
                    color = Color(0xFFFFA000),
                    label = "標準值" ,
                    value = "${String.format("%.1f", standardValue)}ppm",
                    pointStyle = false,
                    isDashed = true,
                    modifier = Modifier.weight(1f)
                )

                // 警報點標示
                if (alarmValue != null) {
                    LegendItem(
                        color = Color(0xFFFF6B6B),
                        label = "警報點",
                        value = "≥${String.format("%.1f", alarmValue)}ppm",
                        isWarning = true,
                        pointStyle = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 主圖表
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // 使用新的 Vico 圖表組件
                COChartContent(
                    chartData = chartData,
                    processedReadings = processedReadings,
                    standardValue = standardValue,
                    deviceType = deviceType,
                    timeFormatter = timeFormatter
                )

                // 圖表說明
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (alarmTime != null) {
                        "*顯示警報時間前後共 ${processedReadings.size} 筆數據"
                    } else {
                        "*顯示最近 ${processedReadings.size} 筆濃度數據"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )

                // 日期顯示
                if (processedReadings.isNotEmpty()) {
                    val dateRange = if (processedReadings.size > 1) {
                        val firstDate = processedReadings.first().readingTime
                        val lastDate = processedReadings.last().readingTime
                        if (firstDate != null && lastDate != null) {
                            val firstDateStr = dateFormatter.format(firstDate)
                            val lastDateStr = dateFormatter.format(lastDate)
                            if (firstDateStr == lastDateStr) "資料日期：$firstDateStr " else "$firstDateStr ~ $lastDateStr"
                        } else "日期不詳"
                    } else {
                        processedReadings.first().readingTime?.let { dateFormatter.format(it) } ?: "日期不詳"
                    }

                    Text(
                        text = "$dateRange",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun COChartContent(
    chartData: Map<String, Map<Int, Number>>,
    processedReadings: List<SensorReading>,
    standardValue: Float,
    deviceType: String,
    timeFormatter: SimpleDateFormat
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    
    // 創建水平線（標準值線）
    val horizontalLine = rememberHorizontalLine(standardValue)
    
    // 創建圖例標籤
    val legendLabels = remember(chartData) {
        chartData.keys.toSet()
    }
    
    LaunchedEffect(chartData) {
        modelProducer.runTransaction {
            lineSeries {
                chartData.forEach { (_, data) ->
                    series(data.keys, data.values)
                }
            }
            extras { extraStore ->
                extraStore[LegendLabelKey] = legendLabels
            }
        }
    }
    
    val lineColors = remember {
        listOf(
            Color(0xFF666666), // 感測器數據 - 灰色
            Color(0xFFFFA000), // 標準值 - 橘色
            Color(0xFFFF6B6B)  // 警報點 - 紅色
        )
    }
    
    val legendItemLabelComponent = rememberTextComponent(vicoTheme.textColor)
    
    CartesianChartHost(
        rememberCartesianChart(
            rememberLineCartesianLayer(
                LineCartesianLayer.LineProvider.series(
                    lineColors.take(chartData.size).map { color ->
                        LineCartesianLayer.rememberLine(
                            fill = LineCartesianLayer.LineFill.single(fill(color)),
                            areaFill = null,
                            pointProvider = LineCartesianLayer.PointProvider.single(
                                LineCartesianLayer.point(
                                    rememberShapeComponent(
                                        fill(color), 
                                        CorneredShape.Pill
                                    )
                                )
                            ),
                        )
                    }
                )
            ),
            startAxis = VerticalAxis.rememberStart(
                title = when (deviceType.lowercase()) {
                    "co" -> "濃度 (ppm)"
                    "co2" -> "濃度 (ppm)"
                    else -> "濃度 (ppm)"
                },
                valueFormatter = { _, value, _ ->
                    String.format("%.1f", value)
                }
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                title = "時間",
                valueFormatter = { _, value, _ ->
                    try {
                        val index = value.toInt()
                        if (processedReadings.isNotEmpty() && index >= 0 && index < processedReadings.size) {
                            val reading = processedReadings[index]
                            reading.readingTime?.let { timeFormatter.format(it) } ?: ""
                        } else {
                            ""
                        }
                    } catch (e: Exception) {
                        ""
                    }
                }
            ),
            decorations = listOf(horizontalLine),
        ),
        modelProducer,
        modifier = Modifier.height(300.dp),
        rememberVicoScrollState(scrollEnabled = false),
    )
}

@Composable
private fun rememberHorizontalLine(standardValue: Float): HorizontalLine {
    val fill = fill(Color(0xFFFFA000))
    val line = rememberLineComponent(fill = fill, thickness = 2.dp)
    return remember(standardValue) {
        HorizontalLine(
            y = { standardValue.toDouble() },
            line = line,
            labelComponent = null,
            verticalLabelPosition = Position.Vertical.Bottom,
        )
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    value: String? = null,
    isWarning: Boolean = false,
    pointStyle: Boolean = false,
    isDashed: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 顏色指示器 - 根據樣式顯示圓點或線條
        if (pointStyle) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        } else {
            // 線條樣式指示器
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(2.dp)
                    .background(
                        color = color,
                        shape = RoundedCornerShape(1.dp)
                    )
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isWarning) FontWeight.Bold else FontWeight.Normal
            )

            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isWarning) color else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// 擴展SensorReading以支持警報點標記
private fun SensorReading.copy(isAlarmPoint: Boolean = false): SensorReading {
    return this.copy(isAlarmPoint = isAlarmPoint)
}

// 嘗試解析警報時間的輔助函數
private fun tryParseAlarmTime(cleanedTime: String, originalTime: String): java.util.Date? {
    val formats = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy/MM/dd HH:mm:ss",
        "yyyy/MM/dd HH:mm",
        "MM/dd/yyyy HH:mm:ss",
        "MM/dd/yyyy HH:mm"
    )

    for (format in formats) {
        try {
            val formatter = SimpleDateFormat(format, Locale.getDefault())
            val parsed = formatter.parse(cleanedTime)
            Log.d("COChart", "警報時間解析成功 (格式: $format): $parsed")
            return parsed
        } catch (e: Exception) {
            // 繼續嘗試下一個格式
        }
    }

    Log.e("COChart", "所有時間格式都解析失敗，原始時間: '$originalTime', 清理後: '$cleanedTime'")
    return null
}