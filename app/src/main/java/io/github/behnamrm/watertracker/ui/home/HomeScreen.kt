package io.github.behnamrm.watertracker.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.behnamrm.watertracker.R
import io.github.behnamrm.watertracker.data.DrinkType
import io.github.behnamrm.watertracker.data.IntakeWithType
import io.github.behnamrm.watertracker.ui.AppViewModelFactory
import io.github.behnamrm.watertracker.util.formatVolume
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

private const val MAX_AMOUNT_ML = 5000

@Composable
fun HomeScreen(
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is HomeEvent.Added ->
                    context.getString(R.string.added_message, formatVolume(event.amountMl), event.typeName)
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Column {
                Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { TodayProgress(state.totalMl, state.goalMl, state.progress) }
        item {
            AddDrinkCard(
                state = state,
                onSelectType = viewModel::selectType,
                onAdd = viewModel::add,
            )
        }
        item {
            Text(
                stringResource(R.string.today_drinks),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (state.entries.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.today_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(state.entries, key = { it.id }) { entry ->
            EntryRow(entry, onDelete = { viewModel.delete(entry) })
        }
    }
}

@Composable
private fun TodayProgress(totalMl: Int, goalMl: Int, progress: Float) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "progress")
    val track = MaterialTheme.colorScheme.primaryContainer
    val bar = MaterialTheme.colorScheme.primary

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 18.dp.toPx()
                val topLeft = Offset(stroke / 2, stroke / 2)
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(track, -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                if (animated > 0f) {
                    drawArc(bar, -90f, 360f * animated, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    formatVolume(totalMl),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.home_goal_of, formatVolume(goalMl)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (progress >= 1f) {
                        stringResource(R.string.home_goal_reached)
                    } else {
                        stringResource(R.string.home_percent, (progress * 100).roundToInt())
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun AddDrinkCard(
    state: HomeUiState,
    onSelectType: (Long) -> Unit,
    onAdd: (Int) -> Unit,
) {
    var amountText by rememberSaveable { mutableStateOf("") }
    val amount = amountText.toIntOrNull()
    val canAdd = state.selectedType != null && amount != null && amount in 1..MAX_AMOUNT_ML
    val focusManager = LocalFocusManager.current

    fun submit() {
        if (!canAdd) return
        onAdd(amount!!)
        amountText = ""
        focusManager.clearFocus()
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.add_drink), style = MaterialTheme.typography.titleMedium)

            if (state.types.isEmpty()) {
                Text(stringResource(R.string.no_drink_types), style = MaterialTheme.typography.bodyMedium)
                return@Column
            }

            // Drink type: dropdown with all types, plus one-tap favorites beside it.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DrinkTypeDropdown(
                    types = state.types,
                    selected = state.selectedType,
                    onSelect = onSelectType,
                    modifier = Modifier.weight(1f),
                )
                state.favorites.forEach { type ->
                    FavoriteTypeButton(type, checked = type.id == state.selectedType?.id, onClick = { onSelectType(type.id) })
                }
            }

            // Amount: free value in ml, plus glass presets that fill it in.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { new -> amountText = new.filter(Char::isDigit).take(4) },
                    label = { Text(stringResource(R.string.amount_ml)) },
                    suffix = { Text("ml") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { submit() },
                    enabled = canAdd,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.size(4.dp))
                    Text(stringResource(R.string.add))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    R.string.preset_third_glass to state.glassMl / 3,
                    R.string.preset_half_glass to state.glassMl / 2,
                    R.string.preset_full_glass to state.glassMl,
                ).forEach { (label, ml) ->
                    PresetButton(
                        label = stringResource(label),
                        amountMl = ml,
                        selected = amount == ml,
                        onClick = { amountText = ml.toString() },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Text(
                stringResource(R.string.time_saved_automatically),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrinkTypeDropdown(
    types: List<DrinkType>,
    selected: DrinkType?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected?.let { "${it.emoji} ${it.name}" }.orEmpty(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.drink_type)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            types.forEach { type ->
                DropdownMenuItem(
                    text = { Text("${type.emoji}  ${type.name}") },
                    onClick = {
                        onSelect(type.id)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoriteTypeButton(type: DrinkType, checked: Boolean, onClick: () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(type.name) } },
        state = rememberTooltipState(),
    ) {
        FilledTonalIconToggleButton(
            checked = checked,
            onCheckedChange = { onClick() },
            modifier = Modifier.size(48.dp),
        ) {
            Text(type.emoji, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun PresetButton(
    label: String,
    amountMl: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        selected = selected,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) colors.secondaryContainer else colors.surface,
        contentColor = if (selected) colors.onSecondaryContainer else colors.onSurface,
        border = BorderStroke(1.dp, if (selected) colors.secondary else colors.outlineVariant),
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 1)
            Text(
                formatVolume(amountMl),
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EntryRow(entry: IntakeWithType, onDelete: () -> Unit) {
    val time = Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault()).toLocalTime()
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    Card {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            leadingContent = {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text(entry.typeEmoji, style = MaterialTheme.typography.titleMedium) }
                }
            },
            headlineContent = { Text(entry.typeName) },
            supportingContent = { Text(time) },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatVolume(entry.amountMl), style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = stringResource(R.string.delete))
                    }
                }
            },
        )
    }
}
