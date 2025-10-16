package com.example.anglecaring.ui.sensorhistory

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.anglecaring.R
import com.example.anglecaring.ui.components.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState

// Device type data class to represent the devices table
data class DeviceInfo(
    val id: Int,
    val type: String,
    val location: String,
    val status: String,
    val expiryDate: Date
)

// List of devices from the devices table
val devicesList = listOf(
    DeviceInfo(1, "co2", "bedroom", "active", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse("2025-05-31 13:26:41") ?: Date()),
    DeviceInfo(2, "co", "bedroom", "active", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse("2025-05-31 13:26:41") ?: Date()),
    DeviceInfo(3, "ir", "bedroom", "active", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse("2025-05-31 13:26:41") ?: Date())
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorHistoryPage(
    onNavigateBack: () -> Unit,
    sensorHistoryViewModel: SensorHistoryViewModel = viewModel()
) {
    val historyState by sensorHistoryViewModel.historyState.collectAsState()
    val selectedDeviceType by sensorHistoryViewModel.selectedDeviceType.collectAsState()
    val selectedLocation by sensorHistoryViewModel.selectedLocation.collectAsState()
    val startDate by sensorHistoryViewModel.startDate.collectAsState()
    val endDate by sensorHistoryViewModel.endDate.collectAsState()
    var showFilterMenu by remember { mutableStateOf(false) }
    
    // Collect unique device IDs, types and locations for filtering
    val uniqueDeviceIds = when (historyState) {
        is SensorHistoryState.Success -> {
            (historyState as SensorHistoryState.Success).readings
                .map { it.deviceId }
                .distinct()
        }
        else -> emptyList()
    }
    
    // Extract unique device types from the devices list
    val uniqueDeviceTypes = devicesList.map { it.type }.distinct()
    
    // Extract unique locations from the devices list
    val uniqueLocations = devicesList.map { it.location }.distinct()
    
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val dateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("感應器歷史紀錄", fontWeight = FontWeight.Bold) },
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
                    IconButton(onClick = { sensorHistoryViewModel.refreshData() }) {
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
                    selectedDeviceType = selectedDeviceType,
                    selectedLocation = selectedLocation,
                    startDate = startDate,
                    endDate = endDate,
                    onDeviceTypeSelected = { deviceType -> 
                        sensorHistoryViewModel.setDeviceTypeFilter(deviceType) 
                    },
                    onLocationSelected = { location ->
                        sensorHistoryViewModel.setLocationFilter(location)
                    },
                    onStartDateSelected = { date ->
                        sensorHistoryViewModel.setStartDate(date)
                    },
                    onEndDateSelected = { date ->
                        sensorHistoryViewModel.setEndDate(date)
                    },
                    onClearFilters = { sensorHistoryViewModel.clearFilters() }
                )
                
                // 數據顯示
                when (historyState) {
                    is SensorHistoryState.Loading -> {
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
                                    text = "載入感應器資料中...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    is SensorHistoryState.Success -> {
                        val readings = (historyState as SensorHistoryState.Success).readings
                        
                        // Apply filters if needed with date filtering
                        val filteredReadings = readings.filter { reading ->
                            val matchesDeviceType = (selectedDeviceType == null || getDeviceTypeFromId(reading.deviceId) == selectedDeviceType)
                            val matchesLocation = (selectedLocation == null || getLocationFromId(reading.deviceId) == selectedLocation)
                            
                            // Date range filter
                            val matchesStartDate = startDate == null || !reading.readingTime.before(startDate)
                            val matchesEndDate = endDate == null || !reading.readingTime.after(endDate)
                            
                            matchesDeviceType && matchesLocation && matchesStartDate && matchesEndDate
                        }

                        // Display total count
                        if (filteredReadings.isNotEmpty()) {
                            DataCountDisplay(
                                count = filteredReadings.size,
                                label = "筆紀錄"
                            )
                        }
                        
                        if (filteredReadings.isEmpty()) {
                            EmptyState(
                                icon = R.drawable.list_24dp,
                                title = "找不到感應器紀錄",
                                subtitle = "請嘗試變更篩選條件或重新整理資料"
                            )
                        } else {
                            // Group readings by date
                            val groupedReadings = filteredReadings.groupBy { reading ->
                                dateOnlyFormat.format(reading.readingTime)
                            }
                            
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                groupedReadings.forEach { (date, readingsForDate) ->
                                    item {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Text(
                                                text = date,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    
                                    items(readingsForDate) { reading ->
                                        val deviceType = getDeviceTypeFromId(reading.deviceId)
                                        val location = getLocationFromId(reading.deviceId)
                                        
                                        SensorReadingItem(
                                            deviceType = deviceType,
                                            location = location,
                                            readingTime = dateFormat.format(reading.readingTime),
                                            booleanValue = reading.booleanValue,
                                            numericValue = reading.numericValue
                                        )
                                    }
                                }
                                
                                // Add bottom spacing
                                item {
                                    Spacer(modifier = Modifier.height(72.dp))
                                }
                            }
                        }
                    }
                    
                    is SensorHistoryState.Error -> {
                        val error = (historyState as SensorHistoryState.Error).message
                        
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
                                    onClick = { sensorHistoryViewModel.refreshData() }
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
    historyState: SensorHistoryState,
    selectedDeviceType: String?,
    selectedLocation: String?,
    startDate: Date?,
    endDate: Date?,
    onDeviceTypeSelected: (String?) -> Unit,
    onLocationSelected: (String?) -> Unit,
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
                contentDescription = "過濾器",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = "感應器過濾選項",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Show active filter count badge with date filters
            if (selectedDeviceType != null || selectedLocation != null || startDate != null || endDate != null) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    // 顯示活躍過濾器數量
                    val filterCount = (if (selectedDeviceType != null) 1 else 0) + 
                                     (if (selectedLocation != null) 1 else 0) +
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
                // 感應器類型過濾器
                Text(
                    text = "感應器類型",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp, top = 4.dp)
                )
                
                // 使用下拉式選單替代 LazyRow 和 FilterChip
                var showSensorTypeDropdown by remember { mutableStateOf(false) }
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSensorTypeDropdown = true }
                            .border(
                                width = if (selectedDeviceType != null) 2.dp else 1.dp,
                                color = if (selectedDeviceType != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (selectedDeviceType != null) 
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
                                if (selectedDeviceType != null) {
                                    Icon(
                                        painter = painterResource(id = getSensorTypeIcon(selectedDeviceType)),
                                        contentDescription = getDeviceTypeDisplay(selectedDeviceType),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                
                                Text(
                                    text = selectedDeviceType?.let { getDeviceTypeDisplay(it) } ?: "選擇感應器類型",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selectedDeviceType != null) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Icon(
                                painter = painterResource(id = R.drawable.keyboard_arrow_down_24dp),
                                contentDescription = "選擇感應器類型",
                                tint = if (selectedDeviceType != null) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    DropdownMenu(
                        expanded = showSensorTypeDropdown,
                        onDismissRequest = { showSensorTypeDropdown = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        // 添加"所有感應器類型"選項
                        DropdownMenuItem(
                            text = { 
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "所有感應器類型",
                                        fontWeight = if (selectedDeviceType == null) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    if (selectedDeviceType == null) {
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
                                onDeviceTypeSelected(null)
                                showSensorTypeDropdown = false
                            }
                        )
                        
                        HorizontalDivider()
                        
                        // IR 感應器選項
                        DropdownMenuItem(
                            text = { 
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.infrared_24dp),
                                        contentDescription = "紅外線感應器",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "紅外線感應器",
                                        fontWeight = if ("ir" == selectedDeviceType) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    if ("ir" == selectedDeviceType) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            painter = painterResource(id = R.drawable.check_24dp),
                                            contentDescription = "已選擇",
                                            tint = Color(0xFF1976D2),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            onClick = { 
                                onDeviceTypeSelected("ir")
                                showSensorTypeDropdown = false
                            }
                        )
                        
                        // CO 感應器選項
                        DropdownMenuItem(
                            text = { 
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.detector_co_24dp),
                                        contentDescription = "一氧化碳感應器",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "一氧化碳感應器",
                                        fontWeight = if ("co" == selectedDeviceType) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    if ("co" == selectedDeviceType) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            painter = painterResource(id = R.drawable.check_24dp),
                                            contentDescription = "已選擇",
                                            tint = Color(0xFFE53935),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            onClick = { 
                                onDeviceTypeSelected("co")
                                showSensorTypeDropdown = false
                            }
                        )
                        
                        // CO2 感應器選項
                        DropdownMenuItem(
                            text = { 
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.co2_24dp),
                                        contentDescription = "二氧化碳感應器",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "二氧化碳感應器",
                                        fontWeight = if ("co2" == selectedDeviceType) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    if ("co2" == selectedDeviceType) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            painter = painterResource(id = R.drawable.check_24dp),
                                            contentDescription = "已選擇",
                                            tint = Color(0xFF388E3C),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            onClick = { 
                                onDeviceTypeSelected("co2")
                                showSensorTypeDropdown = false
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 位置過濾器
                Text(
                    text = "位置",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                
                // 顯示位置選擇下拉選單
                var showDropdown by remember { mutableStateOf(false) }
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDropdown = true }
                            .border(
                                width = if (selectedLocation != null) 2.dp else 1.dp,
                                color = if (selectedLocation != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (selectedLocation != null) 
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
                                    text = selectedLocation?.let { getLocationDisplay(it) } ?: "選擇位置",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selectedLocation != null) MaterialTheme.colorScheme.primary 
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Icon(
                                painter = painterResource(id = R.drawable.keyboard_arrow_down_24dp),
                                contentDescription = "選擇位置",
                                tint = if (selectedLocation != null) 
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    DropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        // 添加"所有位置"選項
                        DropdownMenuItem(
                            text = { 
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "所有位置",
                                        fontWeight = if (selectedLocation == null) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    if (selectedLocation == null) {
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
                                onLocationSelected(null)
                                showDropdown = false
                            }
                        )
                        
                        HorizontalDivider()
                        
                        // 顯示所有可用的位置
                        devicesList.map { it.location }.distinct().forEach { location ->
                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = getLocationDisplay(location),
                                            fontWeight = if (location == selectedLocation) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                        if (location == selectedLocation) {
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
                                    onLocationSelected(location)
                                    showDropdown = false
                                }
                            )
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
                    visible = selectedDeviceType != null || selectedLocation != null || startDate != null || endDate != null,
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
                            if (selectedDeviceType != null) {
                                InputChip(
                                    selected = true,
                                    onClick = { onDeviceTypeSelected(null) },
                                    label = { Text("類型: ${getDeviceTypeDisplay(selectedDeviceType)}") },
                                    trailingIcon = {
                                        Icon(
                                            painterResource(id = R.drawable.close_24dp),
                                            contentDescription = "清除",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = InputChipDefaults.inputChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
                                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        trailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                            
                            if (selectedLocation != null) {
                                InputChip(
                                    selected = true,
                                    onClick = { onLocationSelected(null) },
                                    label = { Text("位置: ${getLocationDisplay(selectedLocation)}") },
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
                                    modifier = Modifier.padding(end = 4.dp)
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
                                        modifier = Modifier.padding(end = 4.dp)
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
                                        modifier = Modifier.padding(end = 4.dp)
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
fun FilterChipsSection(
    deviceType: String?,
    location: String?,
    onClearDeviceType: () -> Unit,
    onClearLocation: () -> Unit
) {
    val hasFilters = deviceType != null || location != null
    
    AnimatedVisibility(
        visible = hasFilters,
        enter = fadeIn(animationSpec = tween(durationMillis = 300)),
        exit = fadeOut(animationSpec = tween(durationMillis = 300))
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (deviceType != null) {
                item {
                    InputChip(
                        selected = true,
                        onClick = { onClearDeviceType() },
                        label = { Text("類型: ${getDeviceTypeDisplay(deviceType)}") },
                        trailingIcon = {
                            Icon(
                                painterResource(id = R.drawable.close_24dp),
                                contentDescription = "清除篩選",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTrailingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }
            
            if (location != null) {
                item {
                    InputChip(
                        selected = true,
                        onClick = { onClearLocation() },
                        label = { Text("位置: ${getLocationDisplay(location)}") },
                        trailingIcon = {
                            Icon(
                                painterResource(id = R.drawable.close_24dp),
                                contentDescription = "清除篩選",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            selectedTrailingIconColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorReadingItem(
    deviceType: String,
    location: String,
    readingTime: String,
    booleanValue: Boolean,
    numericValue: Float?,
) {
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // 時間標示
                Text(
                    text = readingTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 感應器讀數
            if (numericValue != null) {
                Text(
                    text = getReadingValueText(deviceType, numericValue, null),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                // Display boolean value
                Text(
                    text = getReadingValueText(deviceType, 0f, booleanValue),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 位置資訊
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "位置:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = getLocationDisplay(location),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// Helper functions to get device type and location from device ID
private fun getDeviceTypeFromId(deviceId: String): String {
    val device = devicesList.find { it.id.toString() == deviceId.substringAfterLast("-") }
    return device?.type ?: "unknown"
}

private fun getLocationFromId(deviceId: String): String {
    val device = devicesList.find { it.id.toString() == deviceId.substringAfterLast("-") }
    return device?.location ?: "unknown"
}

// Helper function to display friendly device type names
private fun getDeviceTypeDisplay(type: String): String {
    return when (type.lowercase()) {
        "co2" -> "二氧化碳感應器"
        "co" -> "一氧化碳感應器"
        "ir" -> "紅外線感應器"
        else -> type
    }
}

// Helper function to display friendly location names
private fun getLocationDisplay(location: String): String {
    return when (location.lowercase()) {
        "bedroom" -> "臥室"
        "kitchen" -> "廚房"
        "livingroom" -> "客廳"
        "bathroom" -> "浴室"
        else -> location
    }
}

// Helper function to get color for sensor type
@Composable
private fun getSensorTypeColor(type: String): Color {
    return MaterialTheme.colorScheme.primary
}

// Helper function to get icon for sensor type
private fun getSensorTypeIcon(type: String): Int {
    return when (type.lowercase()) {
        "co2" -> R.drawable.co2_24dp
        "co" -> R.drawable.detector_co_24dp
        "ir" -> R.drawable.infrared_24dp
        else -> R.drawable.sensors_24dp
    }
}

// Updated helper function to get formatted reading value text
private fun getReadingValueText(type: String, value: Float, booleanValue: Boolean? = null): String {
    return when (type.lowercase()) {
        "co2" -> "CO₂ 濃度: ${"%.1f".format(value)} ppm"
        "co" -> "CO 濃度: ${"%.1f".format(value)} ppm"
        "ir" -> if (booleanValue != null) {
                   if (booleanValue) "偵測到有人的動靜" else "偵測到無人的動靜"
                } else {
                   "偵測到 ${"%.1f".format(value)} 人"
                }
        else -> "數值: ${"%.1f".format(value)}"
    }
} 