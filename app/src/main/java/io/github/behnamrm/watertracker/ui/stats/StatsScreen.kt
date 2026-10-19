package io.github.behnamrm.watertracker.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.behnamrm.watertracker.R
import io.github.behnamrm.watertracker.data.PeriodStats
import io.github.behnamrm.watertracker.data.StatsMode
import io.github.behnamrm.watertracker.data.TypeShare
import io.github.behnamrm.watertracker.ui.AppViewModelFactory
import io.github.behnamrm.watertracker.ui.components.CircleIconButton
import io.github.behnamrm.watertracker.ui.components.Overline
import io.github.behnamrm.watertracker.ui.components.PillToggle
import io.github.behnamrm.watertracker.ui.components.SoftCard
import io.github.behnamrm.watertracker.ui.components.ThinProgress
import io.github.behnamrm.watertracker.ui.theme.MonoStyle
import io.github.behnamrm.watertracker.util.formatVolumeParts
import io.github.behnamrm.watertracker.util.formatVolume
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun StatsScreen(viewModel: StatsViewModel = viewModel(factory = AppViewModelFactory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val stats = state.stats

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineMedium) }
        item {
            PillToggle(
                options = listOf(
                    stringResource(R.string.stats_hourly),
                    stringResource(R.string.stats_daily),
                    stringResource(R.string.stats_weekly),
                ),
                selectedIndex = StatsMode.entries.indexOf(state.mode),
                onSelect = { viewModel.setMode(StatsMode.entries[it]) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (stats == null) return@LazyColumn

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(
                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    stringResource(R.string.stats_previous),
                    onClick = viewModel::older,
                    size = 40.dp,
                )
                Text(
                    rangeLabel(stats),
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                CircleIconButton(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    stringResource(R.string.stats_next),
                    onClick = viewModel::newer,
                    enabled = state.page > 0,
                    size = 40.dp,
                )
            }
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                Overline(
                    stringResource(
                        when (state.mode) {
                            StatsMode.Hourly -> R.string.stats_per_hour
                            StatsMode.Daily -> R.string.stats_per_day
                            StatsMode.Weekly -> R.string.stats_per_week
                        },
                    ),
                )
                Spacer(Modifier.height(12.dp))
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
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(stringResource(R.string.stats_total), volumeParts(stats.totalMl), Modifier.weight(1f))
                if (state.mode == StatsMode.Hourly) {
                    StatTile(
                        stringResource(R.string.stats_busiest_hour),
                        stats.best?.hour?.let { hourLabel(it) to "" },
                        Modifier.weight(1f),
                    )
                    StatTile(stringResource(R.string.stats_drinks), "${stats.drinkCount}" to "", Modifier.weight(1f))
                } else {
                    StatTile(stringResource(R.string.stats_daily_average), volumeParts(stats.dailyAverageMl), Modifier.weight(1f))
                    StatTile(
                        stringResource(if (state.mode == StatsMode.Daily) R.string.stats_best_day else R.string.stats_best_week),
                        stats.best?.totalMl?.let(::volumeParts),
                        Modifier.weight(1f),
                    )
                }
            }
        }
        if (state.mode == StatsMode.Hourly && state.goalMl > 0) {
            item {
                val fraction = stats.totalMl.toFloat() / state.goalMl
                SoftCard(Modifier.fillMaxWidth()) {
                    Overline(stringResource(R.string.home_goal_caption))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary)) { append("${(fraction * 100).roundToInt()}%") }
                        },
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        stringResource(R.string.stats_of_daily_goal, formatVolume(state.goalMl)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    ThinProgress(fraction, color = MaterialTheme.colorScheme.secondary)
                }
            }
        } else if (state.goalMl > 0 && stats.elapsedDays > 0) {
            item {
                SoftCard(Modifier.fillMaxWidth()) {
                    Overline(stringResource(R.string.home_goal_caption))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary)) { append("${stats.goalDays}") }
                            append(" / ${stats.elapsedDays}")
                        },
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        stringResource(R.string.stats_goal_days_caption),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    ThinProgress(stats.goalDays.toFloat() / stats.elapsedDays, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                Overline(stringResource(R.string.stats_by_type))
                Spacer(Modifier.height(8.dp))
                if (stats.byType.isEmpty()) {
                    Text(
                        stringResource(R.string.stats_no_data),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    stats.byType.forEach { TypeShareRow(it) }
                }
            }
        }
    }
}

/** [value] is a number and its unit, e.g. ("1.25", "L"); null shows a dash. */
@Composable
private fun StatTile(label: String, value: Pair<String, String>?, modifier: Modifier = Modifier) {
    SoftCard(modifier, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)) {
        Overline(label)
        Spacer(Modifier.height(6.dp))
        if (value == null) {
            Text("–", style = MaterialTheme.typography.titleLarge)
        } else {
            val (number, unit) = value
            Row(verticalAlignment = Alignment.Bottom) {
                Text(number, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                Text(
                    unit,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 2.dp, bottom = 3.dp),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun TypeShareRow(share: TypeShare) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Overline("${share.emoji} ${share.name}", modifier = Modifier.weight(1f))
            Text(
                "${formatVolume(share.totalMl)} · ${(share.fraction * 100).roundToInt()}%",
                style = MonoStyle,
            )
        }
        ThinProgress(share.fraction)
    }
}

private fun volumeParts(ml: Int) = formatVolumeParts(ml)

private fun hourLabel(hour: Int): String = LocalTime.of(hour, 0).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

private fun rangeLabel(stats: PeriodStats): String {
    if (stats.range.days == 1L) return stats.range.start.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
    val f = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    return "${stats.range.start.format(f)} – ${stats.range.endExclusive.minusDays(1).format(f)}"
}

private fun chartBars(mode: StatsMode, stats: PeriodStats, today: LocalDate): List<ChartBar> {
    val locale = Locale.getDefault()
    return stats.buckets.map { bucket ->
        val start = bucket.range.start
        when (mode) {
            StatsMode.Hourly -> {
                val hour = bucket.hour ?: 0
                ChartBar(
                    // Label every third hour so the 24 bars stay readable.
                    label = if (hour % 3 == 0) hour.toString() else "",
                    sublabel = "",
                    valueMl = bucket.totalMl,
                    highlighted = start == today && hour == LocalTime.now().hour,
                )
            }
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
