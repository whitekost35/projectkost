package com.kost.project.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeFilter(
    startDate: LocalDate?,
    endDate: LocalDate?,
    onRangeSelected: (LocalDate?, LocalDate?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var pickingStart by remember { mutableStateOf(true) }
    
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id", "ID"))

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Filter Icon
        Surface(
            color = if (startDate != null || endDate != null) Color(0xFFF0F5FA) else Color.White,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(48.dp),
            tonalElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.FilterList,
                    contentDescription = null,
                    tint = if (startDate != null || endDate != null) Color(0xFF5474B3) else Color.Gray
                )
            }
        }

        // Start Date Button
        FilterChip(
            selected = startDate != null,
            onClick = { 
                pickingStart = true
                showDatePicker = true 
            },
            label = { Text(startDate?.format(dateFormatter) ?: "Dari") },
            leadingIcon = { 
                Icon(
                    Icons.Rounded.CalendarMonth, 
                    contentDescription = null, 
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF5474B3)
                ) 
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = FilterChipDefaults.filterChipColors(
                containerColor = Color.White,
                labelColor = Color.Gray,
                iconColor = Color(0xFF5474B3),
                selectedContainerColor = Color(0xFFF0F5FA),
                selectedLabelColor = Color(0xFF5474B3),
                selectedLeadingIconColor = Color(0xFF5474B3)
            )
        )

        Text("s/d", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

        // End Date Button
        FilterChip(
            selected = endDate != null,
            onClick = { 
                pickingStart = false
                showDatePicker = true 
            },
            label = { Text(endDate?.format(dateFormatter) ?: "Sampai") },
            leadingIcon = { 
                Icon(
                    Icons.Rounded.CalendarMonth, 
                    contentDescription = null, 
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF5474B3)
                ) 
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = FilterChipDefaults.filterChipColors(
                containerColor = Color.White,
                labelColor = Color.Gray,
                iconColor = Color(0xFF5474B3),
                selectedContainerColor = Color(0xFFF0F5FA),
                selectedLabelColor = Color(0xFF5474B3),
                selectedLeadingIconColor = Color(0xFF5474B3)
            )
        )

        // Clear Button
        if (startDate != null || endDate != null) {
            IconButton(onClick = { onRangeSelected(null, null) }) {
                Icon(Icons.Rounded.Close, contentDescription = "Reset Filter", tint = Color.Red)
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (pickingStart) {
                startDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
            } else {
                endDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDate = datePickerState.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    if (pickingStart) {
                        onRangeSelected(selectedDate, endDate)
                    } else {
                        onRangeSelected(startDate, selectedDate)
                    }
                    showDatePicker = false
                }) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
