package io.github.behnamrm.watertracker.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.behnamrm.watertracker.R
import io.github.behnamrm.watertracker.data.PeriodStats
import io.github.behnamrm.watertracker.data.StatsMode
import io.github.behnamrm.watertracker.data.TypeShare
import io.github.behnamrm.watertracker.ui.AppViewModelFactory
import io.github.behnamrm.watertracker.util.formatVolume
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = viewModel(factory = AppViewModelFactory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val stats = state.stats

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineMedium) }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                StatsMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = state.mode == mode,
                        onClick = { viewModel.setMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, StatsMode.entries.size),
                    ) {
                        Text(stringResource(if (mode == StatsMode.Daily) R.string.stats_daily else R.string.stats_weekly))
                    }
                }
            }
        }
        if (stats == null) return@LazyColumn

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::older) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.stats_previous))
                }
                Text(
                    rangeLabel(stats),
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = viewModel::newer, enabled = state.page > 0) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.stats_next))
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    BarChart(
                        bars = chartBars(state.mode, stats, state.today),
                        goalMl = state.goalMl.takeIf { state.mode == StatsMode.Daily },
                    )
                    if (state.mode == StatsMode.Daily) {
                        Text(
                            stringResource(R.string.stats_goal_line),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(stringResource(R.string.stats_total), formatVolume(stats.totalMl), Modifier.weight(1f))
                StatTile(stringResource(R.string.stats_daily_average), formatVolume(stats.dailyAverageMl), Modifier.weight(1f))
                StatTile(
                    stringResource(if (state.mode == StatsMode.Daily) R.string.stats_best_day else R.string.stats_best_week),
                    stats.best?.let { formatVolume(it.totalMl) } ?: "–",
                    Modifier.weight(1f),
                )
            }
        }
        if (state.goalMl > 0 && stats.elapsedDays > 0) {
            item {
                Text(
                    stringResource(R.string.stats_goal_days, stats.goalDays, stats.elapsedDays),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item { Text(stringResource(R.string.stats_by_type), style = MaterialTheme.typography.titleMedium) }
        if (stats.byType.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.stats_no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(stats.byType, key = { it.name + it.emoji }) { TypeShareRow(it) }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun TypeShareRow(share: TypeShare) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${share.emoji}  ${share.name}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                "${formatVolume(share.totalMl)} · ${(share.fraction * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(progress = { share.fraction }, modifier = Modifier.fillMaxWidth())
    }
}

private fun rangeLabel(stats: PeriodStats): String {
    val f = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    return "${stats.range.start.format(f)} – ${stats.range.endExclusive.minusDays(1).format(f)}"
}

private fun chartBars(mode: StatsMode, stats: PeriodStats, today: LocalDate): List<ChartBar> {
    val locale = Locale.getDefault()
    return stats.buckets.map { bucket ->
        val start = bucket.range.start
        when (mode) {
            StatsMode.Daily -> ChartBar(
                label = start.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                sublabel = start.dayOfMonth.toString(),
                valueMl = bucket.totalMl,
                highlighted = start == today,
            )
            StatsMode.Weekly -> ChartBar(
                label = start.dayOfMonth.toString(),
                sublabel = start.month.getDisplayName(TextStyle.SHORT, locale),
                valueMl = bucket.totalMl,
                highlighted = today in bucket.range,
            )
        }
    }
}
