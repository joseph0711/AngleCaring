package com.example.anglecaring.ui.components

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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.anglecaring.R

/**
 * 統一的過濾器卡片組件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterCard(
    title: String,
    icon: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    activeFilterCount: Int,
    onClearFilters: () -> Unit,
    content: @Composable () -> Unit
) {
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 0f else 180f,
        animationSpec = tween(durationMillis = 300),
        label = "rotationAnimation"
    )
    
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
                .clickable { onExpandedChange(!expanded) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = "過濾器",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Show active filter count badge
            if (activeFilterCount > 0) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = activeFilterCount.toString(),
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
            content()
        }
    }
}

/**
 * 統一的選擇器組件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorField(
    label: String,
    selectedValue: String?,
    placeholder: String,
    onValueChange: (String?) -> Unit,
    options: List<String>,
    getDisplayText: (String) -> String = { it },
    getColor: (String) -> Color = { Color(0xFF6200EE) },
    showColorIndicator: Boolean = true
) {
    var showDropdown by remember { mutableStateOf(false) }
    
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
    
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDropdown = true }
                .border(
                    width = if (selectedValue != null) 2.dp else 1.dp,
                    color = if (selectedValue != null) getColor(selectedValue) else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(12.dp)
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = if (selectedValue != null) 
                    getColor(selectedValue).copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surface
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
                    if (selectedValue != null && showColorIndicator) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(getColor(selectedValue), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    
                    Text(
                        text = selectedValue?.let { getDisplayText(it) } ?: placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedValue != null) getColor(selectedValue)
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    painter = painterResource(id = R.drawable.keyboard_arrow_down_24dp),
                    contentDescription = "選擇選項",
                    tint = if (selectedValue != null) getColor(selectedValue)
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            // 添加"全部"選項
            DropdownMenuItem(
                text = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "全部",
                            fontWeight = if (selectedValue == null) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (selectedValue == null) {
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
                    onValueChange(null)
                    showDropdown = false
                }
            )
            
            HorizontalDivider()
            
            options.forEach { option ->
                DropdownMenuItem(
                    text = { 
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (showColorIndicator) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(getColor(option), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = getDisplayText(option),
                                fontWeight = if (option == selectedValue) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (option == selectedValue) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    painter = painterResource(id = R.drawable.check_24dp),
                                    contentDescription = "已選擇",
                                    tint = getColor(option),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    onClick = { 
                        onValueChange(option)
                        showDropdown = false
                    }
                )
            }
        }
    }
}

/**
 * 統一的活躍過濾器顯示組件
 */
@Composable
fun ActiveFiltersSection(
    filters: List<FilterChipData>,
    onRemoveFilter: (String) -> Unit
) {
    AnimatedVisibility(
        visible = filters.isNotEmpty(),
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
            
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { filter ->
                    InputChip(
                        selected = true,
                        onClick = { onRemoveFilter(filter.key) },
                        label = { 
                            Text(
                                text = filter.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        },
                        trailingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.close_24dp),
                                contentDescription = "清除",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = filter.color.copy(alpha = 0.12f),
                            labelColor = filter.color,
                            trailingIconColor = filter.color
                        )
                    )
                }
            }
        }
    }
}

/**
 * 統一的空狀態組件
 */
@Composable
fun EmptyState(
    icon: Int,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 32.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 統一的數據計數顯示組件
 */
@Composable
fun DataCountDisplay(
    count: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = "共找到 $count $label",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        textAlign = TextAlign.End
    )
}

/**
 * 過濾器晶片數據類
 */
data class FilterChipData(
    val key: String,
    val label: String,
    val color: Color
)