package com.alertyai.app.ui.tasks.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alertyai.app.ui.components.ClayCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun TaskFilters(
    datePickerDialog: android.app.DatePickerDialog,
    selectedDate: Long?,
    onSelectedDateChange: (Long?) -> Unit,
    currentFilter: String,
    onFilterChange: (String) -> Unit,
    pastTasksFilter: String,
    onPastTasksFilterChange: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Calendar Jump Button
            val isCustomDate = currentFilter == "CUSTOM_DATE"
            ClayCard(
                onClick = { datePickerDialog.show() },
                containerColor = if (isCustomDate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                elevation = if (isCustomDate) 0.dp else 2.dp,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = "Select Date",
                    modifier = Modifier.padding(12.dp),
                    tint = if (isCustomDate) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val filters = listOf("ALL", "PAST", "TODAY", "WEEKLY", "MONTHLY")
            
            filters.forEach { filter ->
                val isActive = currentFilter == filter
                val containerColor = when {
                    isActive && filter == "PAST" -> MaterialTheme.colorScheme.error
                    isActive -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surface
                }
                val textColor = when {
                    isActive && filter == "PAST" -> Color.White
                    isActive -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                
                ClayCard(
                    onClick = { 
                        onFilterChange(filter)
                        if (filter != "CUSTOM_DATE") onSelectedDateChange(null)
                        if (filter == "PAST") onPastTasksFilterChange("ALL")
                    },
                    containerColor = containerColor,
                    elevation = if (isActive) 0.dp else 2.dp,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        filter, 
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }

        AnimatedVisibility(visible = currentFilter == "PAST") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("ALL", "PENDING", "COMPLETED").forEach { f ->
                    val active = pastTasksFilter == f
                    ClayCard(
                        onClick = { onPastTasksFilterChange(f) },
                        containerColor = if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
                        elevation = if (active) 0.dp else 1.dp,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            f,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
