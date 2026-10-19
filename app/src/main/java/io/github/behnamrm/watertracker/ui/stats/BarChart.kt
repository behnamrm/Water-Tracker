package io.github.behnamrm.watertracker.ui.stats

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.unit.sp
import io.github.behnamrm.watertracker.ui.theme.MonoStyle
import io.github.behnamrm.watertracker.util.formatVolumeShort

data class ChartBar(val label: String, val sublabel: String, val valueMl: Int, val highlighted: Boolean)

private val ValueLabelHeight = 18.dp
private val BarAreaHeight = 170.dp
private val BarWidth: Dp = 14.dp

/** A bar chart of rounded capsules on light tracks, with an optional dashed goal line. */
@Composable
fun BarChart(bars: List<ChartBar>, goalMl: Int?, modifier: Modifier = Modifier) {
    val maxValue = maxOf(bars.maxOfOrNull { it.valueMl } ?: 0, goalMl ?: 0, 1)
    val colors = MaterialTheme.colorScheme
    val goalColor = colors.secondary

    // Many bars (24 hours) get thinner bars, tighter spacing and no value labels.
    val dense = bars.size > 12
    val spacing = if (dense) 2.dp else 6.dp
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(BarAreaHeight + ValueLabelHeight)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxSize(),
            ) {
                bars.forEach { bar ->
                    val target = BarAreaHeight * (bar.valueMl.toFloat() / maxValue)
                    val height by animateDpAsState(target, label = "bar")
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            if (bar.valueMl > 0 && !dense) formatVolumeShort(bar.valueMl) else "",
                            style = MonoStyle.copy(fontSize = 10.sp),
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Visible,
                            modifier = Modifier.height(ValueLabelHeight),
                        )
                        Box(
                            contentAlignment = Alignment.BottomCenter,
                            modifier = Modifier
                                .fillMaxWidth(if (dense) 0.8f else 1f)
                                .widthIn(max = BarWidth)
                                .height(BarAreaHeight)
                                .clip(CircleShape)
                                .background(colors.surfaceContainerHigh),
                        ) {
                            if (bar.valueMl > 0) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(maxOf(height, 6.dp))
                                        .clip(CircleShape)
                                        .background(if (bar.highlighted) colors.secondary else colors.primary),
                                )
                            }
                        }
                    }
                }
            }
            if (goalMl != null && goalMl > 0) {
                Canvas(Modifier.fillMaxSize()) {
                    val y = size.height - BarAreaHeight.toPx() * (goalMl.toFloat() / maxValue)
                    drawLine(
                        color = goalColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing), modifier = Modifier.padding(top = 6.dp)) {
            bars.forEach { bar ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        bar.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (bar.highlighted) colors.secondary else colors.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.wrapContentWidth(unbounded = true),
                    )
                    Text(
                        bar.sublabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
