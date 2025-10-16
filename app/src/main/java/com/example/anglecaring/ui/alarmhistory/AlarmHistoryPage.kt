package com.example.anglecaring.ui.alarmhistory

import android.util.Log
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.anglecaring.R
import com.example.anglecaring.data.model.AlarmEvent
import com.example.anglecaring.data.model.AlarmRiskLevel
import com.example.anglecaring.data.model.SensorReading
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import com.example.anglecaring.ui.alarmhistory.COChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmHistoryPage(
    onNavigateBack: () -> Unit,
    onNavigateToAlarmDetail: (AlarmEvent) -> Unit = {},
    alarmHistoryViewModel: AlarmHistoryViewModel = viewModel()
) {
    val historyState by alarmHistoryViewModel.historyState.collectAsState()
    val selectedRiskLevel by alarmHistoryViewModel.selectedRiskLevel.collectAsState()
    val selectedSensor by alarmHistoryViewModel.selectedSensor.collectAsState()
    val startDate by alarmHistoryViewModel.startDate.collectAsState()
    val endDate by alarmHistoryViewModel.endDate.collectAsState()
    val sensorReadings by alarmHistoryViewModel.sensorReadings.collectAsState()
    val expandedAlarms by alarmHistoryViewModel.expandedAlarms.collectAsState()
    val navigationEvent by alarmHistoryViewModel.navigationEvent.collectAsState()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    // 處理導航事件
    LaunchedEffect(navigationEvent) {
        navigationEvent?.let { event ->
            when (event) {
                is AlarmHistoryViewModel.NavigationEvent.NavigateToAlarmDetail -> {
                    val alarms = (historyState as? AlarmHistoryState.Success)?.alarms
                    val alarm = alarms?.find { it.alarmId == event.alarmId }
                    if (alarm != null) {
                        onNavigateToAlarmDetail(alarm)
                    }
                    alarmHistoryViewModel.resetNavigationEvent()
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("警報歷史紀錄") },
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
                    IconButton(onClick = { alarmHistoryViewModel.refreshData() }) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 整合過濾器
                IntegratedFilters(
                    historyState = historyState,
                    selectedRiskLevel = selectedRiskLevel,
                    selectedSensor = selectedSensor,
                    startDate = startDate,
                    endDate = endDate,
                    onRiskLevelSelected = { riskLevel -> 
                        alarmHistoryViewModel.setRiskLevelFilter(riskLevel) 
                    },
                    onSensorSelected = { sensor ->
                        alarmHistoryViewModel.setSensorFilter(sensor)
                    },
                    onStartDateSelected = { date ->
                        alarmHistoryViewModel.setStartDate(date)
                    },
                    onEndDateSelected = { date ->
                        alarmHistoryViewModel.setEndDate(date)
                    },
                    onClearFilters = { alarmHistoryViewModel.clearFilters() }
                )
                
                // 數據顯示
                when (historyState) {
                    is AlarmHistoryState.Loading -> {
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
                                    text = "載入警報資料中...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    is AlarmHistoryState.Success -> {
                        val alarms = (historyState as AlarmHistoryState.Success).alarms
                        
                        // 如果沒有任何警報數據
                        if (alarms.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 32.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.warning_24dp),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Text(
                                        text = "目前沒有任何警報紀錄",
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        text = "當有警報事件發生時，會在此處顯示相關記錄",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        } else {
                            // Apply filters including date range
                            val filteredAlarms = alarms.filter { alarm ->
                                val alarmDate = parseDate(alarm.alarmTime)
                                
                                // Risk level and sensor filter
                                val matchesRiskLevel = selectedRiskLevel == null || alarm.riskLevel == selectedRiskLevel
                                val matchesSensor = selectedSensor == null || alarm.deviceName == selectedSensor
                                
                                // Date range filter
                                val matchesStartDate = startDate == null || (alarmDate != null && !alarmDate.before(startDate))
                                val matchesEndDate = endDate == null || (alarmDate != null && !alarmDate.after(endDate))
                                
                                matchesRiskLevel && matchesSensor && matchesStartDate && matchesEndDate
                            }
                            
                            // Display total count
                            if (filteredAlarms.isNotEmpty()) {
                                Text(
                                    text = "共找到 ${filteredAlarms.size} 筆紀錄",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                            
                            if (filteredAlarms.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = 32.dp),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.warning_24dp),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.size(64.dp)
                                        )
                                        
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        Text(
                                            text = "無找到警報紀錄",
                                            style = MaterialTheme.typography.titleMedium,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Text(
                                            text = "請嘗試清除過濾器或重新整理",
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(filteredAlarms) { alarm ->
                                        // Format the alarm time to remove T and Z characters and ensure HH:MM:SS format
                                        val cleanedTime = alarm.alarmTime
                                            .replace("T", " ")
                                            .replace("Z", "")
                                            .replace(Regex("\\.\\d+"), "") // Remove decimal points and milliseconds
                                        
                                        AlarmItem(
                                            alarm = alarm,
                                            formattedTime = cleanedTime,
                                            formattedCreatedAt = "",
                                            onToggleActive = { isActive ->
                                                alarmHistoryViewModel.updateAlarmStatus(alarm.alarmId, isActive)
                                            },
                                            sensorReadings = sensorReadings[alarm.alarmId],
                                            onExpand = { alarmId ->
                                                alarmHistoryViewModel.toggleAlarmExpanded(alarmId)
                                            },
                                            onViewDetail = {
                                                alarmHistoryViewModel.viewAlarmDetail(alarm.alarmId)
                                            }
                                        )
                                    }
                                    
                                    // 添加底部間距
                                    item {
                                        Spacer(modifier = Modifier.height(72.dp))
                                    }
                                }
                            }
                        }
                    }
                    
                    is AlarmHistoryState.Error -> {
                        val error = (historyState as AlarmHistoryState.Error).message
                        
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
                                    text = "載入警報設定時發生錯誤",
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
                                    onClick = { alarmHistoryViewModel.refreshData() }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegratedFilters(
    historyState: AlarmHistoryState,
    selectedRiskLevel: String?,
    selectedSensor: String?,
    startDate: Date?,
    endDate: Date?,
    onRiskLevelSelected: (String?) -> Unit,
    onSensorSelected: (String?) -> Unit,
    onStartDateSelected: (Date?) -> Unit,
    onEndDateSelected: (Date?) -> Unit,
    onClearFilters: () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 0f else 180f,
        animationSpec = tween(durationMillis = 300),
        label = "rotationAnimation"
    )
    
    // Date picker state
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    // Format for displaying dates
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 6.dp,
            focusedElevation = 5.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        // 過濾器標題列
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.search_24dp),
                contentDescription = "篩選器",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = "警報篩選選項",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Show active filter count badge
            if (selectedRiskLevel != null || selectedSensor != null || startDate != null || endDate != null) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    // Count active filters
                    val filterCount = (if (selectedRiskLevel != null) 1 else 0) + 
                                     (if (selectedSensor != null) 1 else 0) +
                                     (if (startDate != null) 1 else 0) +
                                     (if (endDate != null) 1 else 0)
                    Text(
                        text = filterCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                IconButton(
                    onClick = onClearFilters,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.close_24dp),
                        contentDescription = "清除過濾器",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Icon(
                painter = painterResource(id = R.drawable.keyboard_arrow_down_24dp),
                contentDescription = if (expanded) "收起" else "展開",
                modifier = Modifier.rotate(rotationState)
            )
        }
        
        // 過濾器內容
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // 風險級別過濾器
                Text(
                    text = "風險級別",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp, top = 4.dp)
                )
                
                // 使用 LazyRow 和 FilterChip
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 嚴重
                    item {
                        FilterChip(
                            selected = selectedRiskLevel == AlarmRiskLevel.HIGH,
                            onClick = { 
                                if (selectedRiskLevel == AlarmRiskLevel.HIGH) {
                                    onRiskLevelSelected(null)
                                } else {
                                    onRiskLevelSelected(AlarmRiskLevel.HIGH)
                                }
                            },
                            label = { 
                                Text(
                                    text = "嚴重",
                                    style = MaterialTheme.typography.bodyMedium
                                ) 
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color.Red, CircleShape)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                selectedContainerColor = Color.Red.copy(alpha = 0.12f),
                                selectedLabelColor = Color.Red,
                                selectedLeadingIconColor = Color.Red
                            )
                        )
                    }
                    
                    // 危險
                    item {
                        FilterChip(
                            selected = selectedRiskLevel == AlarmRiskLevel.MEDIUM,
                            onClick = { 
                                if (selectedRiskLevel == AlarmRiskLevel.MEDIUM) {
                                    onRiskLevelSelected(null)
                                } else {
                                    onRiskLevelSelected(AlarmRiskLevel.MEDIUM)
                                }
                            },
                            label = { 
                                Text(
                                    text = "危險",
                                    style = MaterialTheme.typography.bodyMedium
                                ) 
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFFFFA000), CircleShape)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                selectedContainerColor = Color(0xFFFFA000).copy(alpha = 0.12f),
                                selectedLabelColor = Color(0xFFFFA000),
                                selectedLeadingIconColor = Color(0xFFFFA000)
                            )
                        )
                    }
                    
                    // 警告
                    item {
                        FilterChip(
                            selected = selectedRiskLevel == AlarmRiskLevel.LOW,
                            onClick = { 
                                if (selectedRiskLevel == AlarmRiskLevel.LOW) {
                                    onRiskLevelSelected(null)
                                } else {
                                    onRiskLevelSelected(AlarmRiskLevel.LOW)
                                }
                            },
                            label = { 
                                Text(
                                    text = "警告",
                                    style = MaterialTheme.typography.bodyMedium
                                ) 
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 感應器過濾器
                Text(
                    text = "感應器種類",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                
                // 顯示感應器選擇下拉選單
                var showDropdown by remember { mutableStateOf(false) }
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDropdown = true }
                            .border(
                                width = if (selectedSensor != null) 2.dp else 1.dp,
                                color = if (selectedSensor != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (selectedSensor != null) 
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.outlinedCardElevation(
                            defaultElevation = 1.dp,
                            pressedElevation = 2.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedSensor?.let { mapSensorNameToChinese(it) } ?: "選擇感應器",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selectedSensor != null) MaterialTheme.colorScheme.primary 
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Icon(
                                painter = painterResource(id = R.drawable.keyboard_arrow_down_24dp),
                                contentDescription = "選擇感應器",
                                tint = if (selectedSensor != null) 
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    if (showDropdown && historyState is AlarmHistoryState.Success) {
                        val alarms = (historyState as AlarmHistoryState.Success).alarms
                        val uniqueSensors = alarms
                            .mapNotNull { it.deviceName }
                            .distinct()
                            .sortedBy { it }
                        
                        DropdownMenu(
                            expanded = showDropdown,
                            onDismissRequest = { showDropdown = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            // 添加"所有感應器"選項
                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // 添加通用感應器圖標
                                        Icon(
                                            painter = painterResource(id = R.drawable.sensors_24dp),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        
                                        Text(
                                            text = "所有感應器",
                                            fontWeight = if (selectedSensor == null) FontWeight.SemiBold else FontWeight.Normal,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (selectedSensor == null) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                painter = painterResource(id = R.drawable.check_24dp),
                                                contentDescription = "已選擇",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = { 
                                    onSensorSelected(null)
                                    showDropdown = false
                                }
                            )
                            
                            HorizontalDivider()
                            
                            uniqueSensors.forEach { sensor ->
                                DropdownMenuItem(
                                    text = { 
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            // 添加感應器圖標
                                            val iconResId = when (sensor.lowercase()) {
                                                "co" -> R.drawable.detector_co_24dp
                                                "co2" -> R.drawable.co2_24dp
                                                "ir" -> R.drawable.infrared_24dp
                                                else -> null
                                            }
                                            
                                            if (iconResId != null) {
                                                Icon(
                                                    painter = painterResource(id = iconResId),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                            }
                                            
                                            Text(
                                                text = mapSensorNameToChinese(sensor),
                                                fontWeight = if (sensor == selectedSensor) FontWeight.SemiBold else FontWeight.Normal,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (sensor == selectedSensor) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    painter = painterResource(id = R.drawable.check_24dp),
                                                    contentDescription = "已選擇",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = { 
                                        onSensorSelected(sensor)
                                        showDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Add date range filters
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = "日期範圍",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                
                // Start date selector
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStartDatePicker = true }
                        .border(
                            width = if (startDate != null) 2.dp else 1.dp,
                            color = if (startDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (startDate != null) 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.outlinedCardElevation(
                        defaultElevation = 1.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = startDate?.let { dateFormatter.format(it) } ?: "選擇開始日期",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (startDate != null) MaterialTheme.colorScheme.primary 
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        if (startDate != null) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.close_24dp),
                                contentDescription = "清除日期",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { 
                                        onStartDateSelected(null)
                                    }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // End date selector
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEndDatePicker = true }
                        .border(
                            width = if (endDate != null) 2.dp else 1.dp,
                            color = if (endDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (endDate != null) 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.outlinedCardElevation(
                        defaultElevation = 1.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = endDate?.let { dateFormatter.format(it) } ?: "選擇結束日期",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (endDate != null) MaterialTheme.colorScheme.primary 
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        if (endDate != null) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.close_24dp),
                                contentDescription = "清除日期",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { 
                                        onEndDateSelected(null)
                                    }
                            )
                        }
                    }
                }
                
                // 顯示當前活躍的過濾器
                AnimatedVisibility(
                    visible = selectedRiskLevel != null || selectedSensor != null || startDate != null || endDate != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp)
                    ) {
                        Text(
                            text = "活躍過濾器",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )
                        
                        // 使用 InputChip 顯示已選擇的過濾器
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (selectedRiskLevel != null) {
                                val (chipText, chipColor) = when(selectedRiskLevel) {
                                    AlarmRiskLevel.HIGH -> Pair("嚴重", Color.Red)
                                    AlarmRiskLevel.MEDIUM -> Pair("危險", Color(0xFFFFA000))
                                    AlarmRiskLevel.LOW -> Pair("警告", MaterialTheme.colorScheme.primary)
                                    else -> Pair("", MaterialTheme.colorScheme.primary)
                                }
                                
                                InputChip(
                                    selected = true,
                                    onClick = { onRiskLevelSelected(null) },
                                    label = { Text("風險: $chipText") },
                                    trailingIcon = {
                                        Icon(
                                            painterResource(id = R.drawable.close_24dp),
                                            contentDescription = "清除",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = InputChipDefaults.inputChipColors(
                                        containerColor = chipColor.copy(alpha = 0.12f),
                                        labelColor = chipColor,
                                        trailingIconColor = chipColor
                                    ),
                                    modifier = Modifier.wrapContentWidth()
                                )
                            }
                            
                            if (selectedSensor != null) {
                                InputChip(
                                    selected = true,
                                    onClick = { onSensorSelected(null) },
                                    label = { 
                                        Text(
                                            text = "感應器: ${mapSensorNameToChinese(selectedSensor)}",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        ) 
                                    },
                                    trailingIcon = {
                                        Icon(
                                            painterResource(id = R.drawable.close_24dp),
                                            contentDescription = "清除",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = InputChipDefaults.inputChipColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        trailingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier.wrapContentWidth()
                                )
                            }
                        }
                        
                        // Date range chips
                        if (startDate != null || endDate != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (startDate != null) {
                                    InputChip(
                                        selected = true,
                                        onClick = { onStartDateSelected(null) },
                                        label = { 
                                            Text(
                                                text = "從: ${dateFormatter.format(startDate)}",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            ) 
                                        },
                                        trailingIcon = {
                                            Icon(
                                                painterResource(id = R.drawable.close_24dp),
                                                contentDescription = "清除",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        colors = InputChipDefaults.inputChipColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                            trailingIconColor = MaterialTheme.colorScheme.onTertiaryContainer
                                        ),
                                        modifier = Modifier.wrapContentWidth()
                                    )
                                }
                                
                                if (endDate != null) {
                                    InputChip(
                                        selected = true,
                                        onClick = { onEndDateSelected(null) },
                                        label = { 
                                            Text(
                                                text = "至: ${dateFormatter.format(endDate)}",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            ) 
                                        },
                                        trailingIcon = {
                                            Icon(
                                                painterResource(id = R.drawable.close_24dp),
                                                contentDescription = "清除",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        colors = InputChipDefaults.inputChipColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                            trailingIconColor = MaterialTheme.colorScheme.onTertiaryContainer
                                        ),
                                        modifier = Modifier.wrapContentWidth()
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
    
    // Start date picker dialog
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate?.time ?: System.currentTimeMillis()
        )

        // 使用標準的 DatePickerDialog，避免使用全限定名稱
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(24.dp),
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { timeInMillis ->
                            val calendar = java.util.Calendar.getInstance()
                            calendar.timeInMillis = timeInMillis
                            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                            calendar.set(java.util.Calendar.MINUTE, 0)
                            calendar.set(java.util.Calendar.SECOND, 0)
                            calendar.set(java.util.Calendar.MILLISECOND, 0)
                            onStartDateSelected(calendar.time)
                        }
                        showStartDatePicker = false
                    },
                    modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                ) {
                    Text("確認")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showStartDatePicker = false },
                    modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                ) {
                    Text("取消")
                }
            },
        ) {
            DatePicker(
                state = datePickerState,
                title = { 
                    Text(
                        text = "選擇開始日期",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                    ) 
                },
                headline = {
                    val selectedDate = datePickerState.selectedDateMillis?.let {
                        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = it }
                        "${calendar.get(java.util.Calendar.YEAR)}年${calendar.get(java.util.Calendar.MONTH) + 1}月${calendar.get(java.util.Calendar.DAY_OF_MONTH)}日"
                    } ?: "請選擇日期"
                    Text(
                        text = selectedDate,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 8.dp)
                    )
                },
                showModeToggle = true,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
    
    // End date picker dialog
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate?.time ?: System.currentTimeMillis()
        )

        // 使用標準的 DatePickerDialog，避免使用全限定名稱
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(24.dp),
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { timeInMillis ->
                            val calendar = java.util.Calendar.getInstance()
                            calendar.timeInMillis = timeInMillis
                            calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
                            calendar.set(java.util.Calendar.MINUTE, 59)
                            calendar.set(java.util.Calendar.SECOND, 59)
                            calendar.set(java.util.Calendar.MILLISECOND, 999)
                            onEndDateSelected(calendar.time)
                        }
                        showEndDatePicker = false
                    },
                    modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                ) {
                    Text("確認")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showEndDatePicker = false },
                    modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                ) {
                    Text("取消")
                }
            },
        ) {
            DatePicker(
                state = datePickerState,
                title = { 
                    Text(
                        text = "選擇結束日期",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                    ) 
                },
                headline = {
                    val selectedDate = datePickerState.selectedDateMillis?.let {
                        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = it }
                        "${calendar.get(java.util.Calendar.YEAR)}年${calendar.get(java.util.Calendar.MONTH) + 1}月${calendar.get(java.util.Calendar.DAY_OF_MONTH)}日"
                    } ?: "請選擇日期"
                    Text(
                        text = selectedDate,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 8.dp)
                    )
                },
                showModeToggle = true,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
fun AlarmItem(
    alarm: AlarmEvent,
    formattedTime: String,
    formattedCreatedAt: String,
    onToggleActive: (Boolean) -> Unit,
    sensorReadings: List<SensorReading>? = null,
    onExpand: (Int) -> Unit = {},
    onViewDetail: (Int) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 3.dp,
            pressedElevation = 6.dp,
            hoveredElevation = 4.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 風險等級指示器
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = when (alarm.riskLevel) {
                                AlarmRiskLevel.HIGH -> Color.Red
                                AlarmRiskLevel.MEDIUM -> Color(0xFFFFA000) // 橙色
                                else -> MaterialTheme.colorScheme.primary // 警告
                            },
                            shape = CircleShape
                        )
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // 風險等級標籤
                val riskText = when (alarm.riskLevel) {
                    AlarmRiskLevel.HIGH -> "嚴重"
                    AlarmRiskLevel.MEDIUM -> "危險"
                    AlarmRiskLevel.LOW -> "警告"
                    else -> ""
                }
                
                Text(
                    text = riskText,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (alarm.riskLevel) {
                        AlarmRiskLevel.HIGH -> Color.Red
                        AlarmRiskLevel.MEDIUM -> Color(0xFFFFA000)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // 時間標示
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 警報標題
            Text(
                text = alarm.alarmLabel ?: "未命名警報",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 感應器資訊
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // 顯示感應器名稱
                    if (alarm.deviceName != null) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "感應器:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = alarm.deviceName?.let { mapSensorNameToChinese(it) } ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                
                                // 添加感應器圖標
                                alarm.deviceName?.let { deviceName ->
                                    val iconResId = when (deviceName.lowercase()) {
                                        "co" -> R.drawable.detector_co_24dp
                                        "co2" -> R.drawable.co2_24dp
                                        "ir" -> R.drawable.infrared_24dp
                                        else -> null
                                    }
                                    
                                    if (iconResId != null) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            painter = painterResource(id = iconResId),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // 顯示感應器位置
                    if (alarm.deviceLocation != null) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "位置:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = alarm.deviceLocation?.let { mapLocationToChinese(it) } ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // 顯示感應器數值
                    if (alarm.value != null) {
                        // 根據感應器類型顯示適當的單位和格式
                        val formattedValue = when (alarm.deviceName?.lowercase()) {
                            "co" -> "${String.format("%.1f", alarm.value)} ppm"
                            "co2" -> "${String.format("%.1f", alarm.value)} ppm"
                            "ir" -> "偵測到移動"
                            else -> alarm.value.toString()
                        }
                        
                        // 根據數值和標準值比較來設定顏色
                        val valueColor = if (alarm.standardValue != null && alarm.value > alarm.standardValue) {
                            when (alarm.riskLevel) {
                                AlarmRiskLevel.HIGH -> Color.Red
                                AlarmRiskLevel.MEDIUM -> Color(0xFFFFA000)
                                else -> MaterialTheme.colorScheme.primary
                            }
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                        
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "檢測值:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = formattedValue,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = valueColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                                
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // 顯示標準值（如果存在）
                    if (alarm.standardValue != null && alarm.deviceName?.lowercase() != "ir") {
                        val formattedStandardValue = when (alarm.deviceName?.lowercase()) {
                            "co" -> "≤ ${String.format("%.1f", alarm.standardValue)} ppm"
                            "co2" -> "≤ ${String.format("%.1f", alarm.standardValue)} ppm"
                            else -> alarm.standardValue.toString()
                        }
                        
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "安全值:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = formattedStandardValue,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        // 添加危險程度提示（僅針對超標的數值）
                        if (alarm.value != null && alarm.value > alarm.standardValue) {
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            val dangerRatio = alarm.value / alarm.standardValue
                            val dangerText = when {
                                dangerRatio >= 1.5f -> "嚴重超標"
                                dangerRatio >= 1.2f -> "中度超標"
                                dangerRatio >= 1.0f -> "輕微超標"
                                else -> ""
                            }
                        }
                    }
                }
            }
            
            // 查看詳情按鈕，為CO和CO2類型的警報顯示
            if (alarm.deviceName == "co" || alarm.deviceName == "co2") {
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { 
                        Log.d("AlarmHistoryPage", "查看詳情按鈕被點擊，警報ID: ${alarm.alarmId}, 設備類型: ${alarm.deviceName}")
                        onViewDetail(alarm.alarmId)
                    },
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("查看詳情")
                }
            }
        }
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

// Parse date from string
private fun parseDate(dateString: String): Date? {
    return try {
        // Remove T, Z and milliseconds from the date string
        val cleanedTime = dateString
            .replace("T", " ")
            .replace("Z", "")
            .replace(Regex("\\.\\d+"), "")
        
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(cleanedTime)
    } catch (e: Exception) {
        null
    }
} 