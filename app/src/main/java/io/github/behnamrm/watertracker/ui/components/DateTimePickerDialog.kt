package io.github.behnamrm.watertracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.behnamrm.watertracker.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * One dialog for picking both a date and a time. Two pills at the top switch between the
 * calendar and the clock; choosing a day moves on to the clock. Nothing in the future can be picked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialog(
    initial: LocalDateTime,
    onDismiss: () -> Unit,
    onUseNow: () -> Unit,
    onConfirm: (LocalDateTime) -> Unit,
) {
    val today = LocalDate.now()
    val todayUtcMillis = today.toUtcMillis()
    var showClock by rememberSaveable { mutableStateOf(false) }

    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = initial.toLocalDate().toUtcMillis(),
        selectableDates = remember(todayUtcMillis) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayUtcMillis
                override fun isSelectableYear(year: Int) = year <= today.year
            }
        },
    )
    val timeState = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute)

    val date = dateState.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
        ?: initial.toLocalDate()
    val dateTime = date.atTime(timeState.hour, timeState.minute)
    val inFuture = dateTime.isAfter(LocalDateTime.now())

    // Picking a day moves straight on to the time.
    var lastDateMillis by rememberSaveable { mutableStateOf(dateState.selectedDateMillis) }
    LaunchedEffect(dateState.selectedDateMillis) {
        if (dateState.selectedDateMillis != lastDateMillis) {
            lastDateMillis = dateState.selectedDateMillis
            showClock = true
        }
    }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(dateTime) }, enabled = !inFuture) { Text(stringResource(R.string.done)) }
        },
        dismissButton = {
            TextButton(onClick = onUseNow) { Text(stringResource(R.string.date_use_now)) }
        },
    ) {
        Column {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
            ) {
                PickerPill(
                    icon = Icons.Outlined.CalendarMonth,
                    text = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                    selected = !showClock,
                    onClick = { showClock = false },
                    modifier = Modifier.weight(1f),
                )
                PickerPill(
                    icon = Icons.Outlined.Schedule,
                    text = dateTime.toLocalTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
                    selected = showClock,
                    onClick = { showClock = true },
                    modifier = Modifier.weight(1f),
                )
            }
            if (showClock) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    TimePicker(timeState)
                }
                if (inFuture) {
                    Text(
                        stringResource(R.string.time_in_future),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
            } else {
                DatePicker(dateState, title = null, headline = null, showModeToggle = false)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun PickerPill(
    icon: ImageVector,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) colors.primary else colors.surfaceContainerHighest,
        contentColor = if (selected) colors.onPrimary else colors.onSurface,
        modifier = modifier.height(48.dp).semantics { this.selected = selected },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

private fun LocalDate.toUtcMillis() = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
