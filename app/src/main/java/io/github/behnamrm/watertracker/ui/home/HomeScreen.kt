package io.github.behnamrm.watertracker.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.behnamrm.watertracker.R
import io.github.behnamrm.watertracker.data.IntakeWithType
import io.github.behnamrm.watertracker.ui.AppViewModelFactory
import io.github.behnamrm.watertracker.ui.components.CircleIconButton
import io.github.behnamrm.watertracker.ui.components.Overline
import io.github.behnamrm.watertracker.ui.components.SoftCard
import io.github.behnamrm.watertracker.ui.components.ThinProgress
import io.github.behnamrm.watertracker.ui.theme.MonoStyle
import io.github.behnamrm.watertracker.util.formatNumber
import io.github.behnamrm.watertracker.util.formatVolume
import io.github.behnamrm.watertracker.util.formatVolumeParts
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    snackbarHostState: SnackbarHostState,
    onOpenStats: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is HomeEvent.Added -> {
                    val date = Instant.ofEpochMilli(event.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                    if (date == LocalDate.now()) {
                        context.getString(R.string.added_message, formatVolume(event.amountMl), event.typeName)
                    } else {
                        context.getString(
                            R.string.added_message_on,
                            formatVolume(event.amountMl),
                            event.typeName,
                            date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                        )
                    }
                }
                is HomeEvent.Deleted ->
                    context.getString(R.string.deleted_message, formatVolume(event.entry.amountMl), event.entry.typeName)
            }
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = context.getString(R.string.undo),
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undo(event)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium)
                    Text(
                        state.today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                CircleIconButton(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = stringResource(R.string.tab_stats),
                    onClick = onOpenStats,
                    size = 40.dp,
                )
            }
        }
        item { WeekStrip(state) }
        item { TodayCard(state) }
        item {
            AddDrinkPanel(
                state = state,
                onSelectType = viewModel::selectType,
                onAdd = viewModel::add,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                GoalCard(state, Modifier.weight(1f).fillMaxHeight())
                ByDrinkCard(state, Modifier.weight(1f).fillMaxHeight())
            }
        }
        item {
            Overline(stringResource(R.string.today_drinks), modifier = Modifier.padding(start = 4.dp, top = 8.dp))
        }
        if (state.entries.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.today_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        } else {
            item {
                SoftCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                    state.entries.forEachIndexed { index, entry ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
                        EntryRow(entry, onDelete = { viewModel.delete(entry) })
                    }
                }
            }
        }
    }
}

/** One circle per day of this week: dark blue when the goal was reached, light blue for today. */
@Composable
private fun WeekStrip(state: HomeUiState) {
    val colors = MaterialTheme.colorScheme
    val locale = Locale.getDefault()
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        state.week.forEach { day ->
            val isToday = day.date == state.today
            val reached = state.goalMl > 0 && day.totalMl >= state.goalMl
            val future = day.date.isAfter(state.today)
            val (fill, text) = when {
                isToday -> colors.secondary to colors.onSecondary
                reached -> colors.primary to colors.onPrimary
                future -> colors.surfaceContainerHigh to colors.onSurfaceVariant
                else -> colors.surface to colors.onSurface
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(
                    day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(fill),
                ) {
                    Text(
                        day.date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = text,
                    )
                }
            }
        }
    }
}

/** Today's total in large type, with this week's days as thin bars. */
@Composable
private fun TodayCard(state: HomeUiState) {
    val colors = MaterialTheme.colorScheme
    SoftCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Overline(stringResource(R.string.home_today_caption))
                Spacer(Modifier.height(6.dp))
                val (number, unit) = formatVolumeParts(state.totalMl)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(number, style = MaterialTheme.typography.displayMedium)
                    Text(
                        unit,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    buildAnnotatedString {
                        append(stringResource(R.string.home_week_avg) + " ")
                        withStyle(SpanStyle(color = colors.secondary, fontWeight = FontWeight.SemiBold)) {
                            append(formatVolume(state.weekAverageMl))
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            WeekBars(state, Modifier.width(132.dp).height(104.dp))
        }
    }
}

@Composable
private fun WeekBars(state: HomeUiState, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val max = maxOf(state.goalMl, state.week.maxOfOrNull { it.totalMl } ?: 0, 1)
    val locale = Locale.getDefault()
    Column(modifier) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            state.week.forEach { day ->
                val fraction by animateFloatAsState(day.totalMl.toFloat() / max, label = "weekBar")
                val isToday = day.date == state.today
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier
                        .width(9.dp)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(colors.surfaceContainerHigh),
                ) {
                    if (fraction > 0f) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction.coerceIn(0.08f, 1f))
                                .clip(CircleShape)
                                .background(if (isToday) colors.secondary else colors.primary),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            state.week.forEach { day ->
                Text(
                    day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.width(9.dp).wrapContentWidth(unbounded = true),
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

/** A 270° gauge with the share of the daily goal. */
@Composable
private fun GoalCard(state: HomeUiState, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val animated by animateFloatAsState(state.progress.coerceIn(0f, 1f), label = "goal")
    SoftCard(modifier) {
        Overline(stringResource(R.string.home_goal_caption))
        Spacer(Modifier.height(12.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.align(Alignment.CenterHorizontally).size(104.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 12.dp.toPx()
                val topLeft = Offset(stroke / 2, stroke / 2)
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(colors.surfaceContainerHigh, 135f, 270f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                if (animated > 0f) {
                    drawArc(colors.primary, 135f, 270f * animated, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
            Text(
                if (state.progress >= 1f) "🎉" else "${(state.progress * 100).roundToInt()}%",
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = colors.secondary)) { append(formatNumber(state.totalMl)) }
                append(" / ${formatNumber(state.goalMl)}")
            },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Text(
            "ml",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

/** Today's amount for the main drink types, as thin lines against the daily goal. */
@Composable
private fun ByDrinkCard(state: HomeUiState, modifier: Modifier) {
    SoftCard(modifier) {
        Overline(stringResource(R.string.home_by_drink))
        Spacer(Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.weight(1f)) {
            state.todayByType.forEach { type ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Overline("${type.emoji} ${type.name}", modifier = Modifier.weight(1f))
                        Text(formatVolume(type.totalMl), style = MonoStyle)
                    }
                    ThinProgress(if (state.goalMl > 0) type.totalMl.toFloat() / state.goalMl else 0f)
                }
            }
        }
    }
}

@Composable
private fun EntryRow(entry: IntakeWithType, onDelete: () -> Unit) {
    val time = Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault()).toLocalTime()
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) { Text(entry.typeEmoji, style = MaterialTheme.typography.titleMedium) }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(entry.typeName, style = MaterialTheme.typography.titleSmall)
            Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(formatVolume(entry.amountMl), style = MonoStyle.copy(fontWeight = FontWeight.Medium))
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
