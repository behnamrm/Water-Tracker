package io.github.behnamrm.watertracker.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.behnamrm.watertracker.R
import io.github.behnamrm.watertracker.data.DrinkType
import io.github.behnamrm.watertracker.ui.components.AmountDialog
import io.github.behnamrm.watertracker.ui.components.DateTimePickerDialog
import io.github.behnamrm.watertracker.ui.components.Overline
import io.github.behnamrm.watertracker.ui.components.SoftCard
import io.github.behnamrm.watertracker.util.formatVolume
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Amounts offered in the capacity dropdown. */
private val CapacityChoicesMl = listOf(100, 150, 200, 300, 330, 500, 750, 1000)

private val TileHeight = 76.dp
private val BottomRowHeight = 56.dp
private val TileShape = RoundedCornerShape(16.dp)

/**
 * The add panel, laid out as three rows:
 *
 *     [Type ▾]     [Water] [Soft drink] [Milk]
 *     [Capacity ▾] [⅓]     [Half glass] [Full]
 *     [Date]       [          Add          ]
 */
@Composable
fun AddDrinkPanel(
    state: HomeUiState,
    onSelectType: (Long) -> Unit,
    onAdd: (amountMl: Int, timestamp: Long?) -> Unit,
) {
    val glassMl = state.glassMl
    // null means "full glass", so the default follows the glass size set in Settings.
    var chosenMl by rememberSaveable { mutableStateOf<Int?>(null) }
    val amountMl = chosenMl ?: glassMl
    // null means "now".
    var chosenTime by rememberSaveable { mutableStateOf<Long?>(null) }

    SoftCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Overline(stringResource(R.string.add_drink), modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp))
            if (state.types.isEmpty()) {
                Text(stringResource(R.string.no_drink_types), style = MaterialTheme.typography.bodyMedium)
                return@Column
            }

            TypeRow(state, onSelectType)
            CapacityRow(glassMl, amountMl, onAmountChange = { chosenMl = it })

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateTile(
                    timestamp = chosenTime,
                    onChange = { chosenTime = it },
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        onAdd(amountMl, chosenTime)
                        chosenTime = null
                    },
                    enabled = state.selectedType != null,
                    shape = CircleShape,
                    modifier = Modifier.weight(3f).height(BottomRowHeight),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.add_amount, formatVolume(amountMl)),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeRow(state: HomeUiState, onSelectType: (Long) -> Unit) {
    val selected = state.selectedType
    val selectedIsFavorite = state.favorites.any { it.id == selected?.id }
    var menuOpen by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f)) {
            Tile(selected = !selectedIsFavorite && selected != null, onClick = { menuOpen = true }) {
                if (!selectedIsFavorite && selected != null) {
                    WithDropdownArrow { Text(selected.emoji, style = MaterialTheme.typography.headlineSmall) }
                    TileLabel(selected.name)
                } else {
                    WithDropdownArrow { Icon(Icons.Rounded.LocalDrink, contentDescription = null, modifier = Modifier.size(28.dp)) }
                    TileLabel(stringResource(R.string.drink_type))
                }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                state.types.forEach { type ->
                    DropdownMenuItem(
                        text = { Text("${type.emoji}  ${type.name}") },
                        onClick = {
                            onSelectType(type.id)
                            menuOpen = false
                        },
                    )
                }
            }
        }
        state.favorites.forEach { type ->
            TypeTile(type, selected = type.id == selected?.id, onClick = { onSelectType(type.id) }, Modifier.weight(1f))
        }
        // Keep the grid aligned when fewer than three favorites are starred.
        repeat(HomeUiState.MAX_FAVORITES - state.favorites.size) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun TypeTile(type: DrinkType, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Tile(selected = selected, onClick = onClick, modifier = modifier) {
        Text(type.emoji, style = MaterialTheme.typography.headlineSmall)
        TileLabel(type.name)
    }
}

@Composable
private fun CapacityRow(glassMl: Int, amountMl: Int, onAmountChange: (Int) -> Unit) {
    val presets = listOf(
        Triple(R.string.preset_third_glass, glassMl / 3, 1f / 3),
        Triple(R.string.preset_half_glass, glassMl / 2, 0.5f),
        Triple(R.string.preset_full_glass, glassMl, 1f),
    )
    val isPreset = presets.any { it.second == amountMl }
    var menuOpen by remember { mutableStateOf(false) }
    var customDialog by rememberSaveable { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f)) {
            Tile(selected = !isPreset, onClick = { menuOpen = true }) {
                if (isPreset) {
                    WithDropdownArrow { Icon(Icons.Rounded.Straighten, contentDescription = null, modifier = Modifier.size(28.dp)) }
                } else {
                    WithDropdownArrow {
                        Text(
                            formatVolume(amountMl),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }
                TileLabel(stringResource(R.string.capacity))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                CapacityChoicesMl.forEach { ml ->
                    DropdownMenuItem(
                        text = { Text(formatVolume(ml)) },
                        onClick = {
                            onAmountChange(ml)
                            menuOpen = false
                        },
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.capacity_custom)) },
                    onClick = {
                        menuOpen = false
                        customDialog = true
                    },
                )
            }
        }
        presets.forEach { (label, ml, fill) ->
            Tile(selected = amountMl == ml, onClick = { onAmountChange(ml) }, modifier = Modifier.weight(1f)) {
                GlassIcon(fill)
                TileLabel(stringResource(label))
                Text(
                    formatVolume(ml),
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalContentColor.current.copy(alpha = 0.6f),
                    maxLines = 1,
                )
            }
        }
    }

    if (customDialog) {
        AmountDialog(
            title = stringResource(R.string.capacity),
            confirmLabel = stringResource(R.string.ok),
            initialMl = amountMl,
            onDismiss = { customDialog = false },
            onConfirm = {
                onAmountChange(it)
                customDialog = false
            },
        )
    }
}

/** Date & time tile: "Now" by default; tapping it opens one dialog to pick both (not in the future). */
@Composable
private fun DateTile(timestamp: Long?, onChange: (Long?) -> Unit, modifier: Modifier) {
    val zone = ZoneId.systemDefault()
    var picking by rememberSaveable { mutableStateOf(false) }

    Tile(selected = timestamp != null, onClick = { picking = true }, modifier = modifier, height = BottomRowHeight, shape = CircleShape) {
        if (timestamp == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                TileLabel(stringResource(R.string.date_now))
            }
        } else {
            val dateTime = Instant.ofEpochMilli(timestamp).atZone(zone)
            TileLabel(dateTime.format(DateTimeFormatter.ofPattern("d MMM")))
            Text(
                dateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }

    if (picking) {
        DateTimePickerDialog(
            initial = timestamp?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime() } ?: LocalDateTime.now(),
            onDismiss = { picking = false },
            onUseNow = {
                onChange(null)
                picking = false
            },
            onConfirm = { picked ->
                onChange(picked.atZone(zone).toInstant().toEpochMilli())
                picking = false
            },
        )
    }
}

@Composable
private fun Tile(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = TileHeight,
    shape: androidx.compose.ui.graphics.Shape = TileShape,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (selected) colors.primary else colors.surfaceContainer,
        contentColor = if (selected) colors.onPrimary else colors.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { this.selected = selected },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            content = content,
        )
    }
}

@Composable
private fun TileLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
    )
}

/** Puts a small ▾ next to the tile's graphic, marking it as a dropdown. */
@Composable
private fun WithDropdownArrow(graphic: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        graphic()
        Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
    }
}

/** A drinking glass filled to [fill] (0..1). */
@Composable
private fun GlassIcon(fill: Float, modifier: Modifier = Modifier.size(width = 24.dp, height = 28.dp)) {
    val outline = LocalContentColor.current
    val water = MaterialTheme.colorScheme.secondary
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val taper = w * 0.14f
        val glass = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w - taper, h)
            lineTo(taper, h)
            close()
        }
        clipPath(glass) {
            val top = h * (1f - fill)
            drawRect(water, topLeft = Offset(0f, top), size = Size(w, h - top))
            // A lighter band at the surface makes it read as liquid.
            drawRect(water.copy(alpha = 0.45f), topLeft = Offset(0f, top - h * 0.06f), size = Size(w, h * 0.06f))
        }
        drawPath(glass, outline, style = Stroke(width = 1.8.dp.toPx(), join = StrokeJoin.Round))
    }
}
