package io.github.behnamrm.watertracker.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import io.github.behnamrm.watertracker.ui.components.Overline
import io.github.behnamrm.watertracker.ui.components.SoftCard
import io.github.behnamrm.watertracker.ui.theme.MonoStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.behnamrm.watertracker.R
import io.github.behnamrm.watertracker.data.DrinkType
import io.github.behnamrm.watertracker.ui.AppViewModelFactory
import io.github.behnamrm.watertracker.ui.components.AmountDialog
import io.github.behnamrm.watertracker.ui.home.HomeUiState
import io.github.behnamrm.watertracker.util.formatVolume
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val EmojiChoices = listOf("💧", "🥤", "🧃", "🍺", "🍷", "🍵", "☕", "🥛", "🍹", "🥥", "🍶", "🥃")

private sealed interface SettingsDialog {
    data object Goal : SettingsDialog
    data object Glass : SettingsDialog
    data object AddType : SettingsDialog
    data class EditType(val type: DrinkType) : SettingsDialog
    data class RemoveType(val type: DrinkType) : SettingsDialog
    data object ConfirmImport : SettingsDialog
}

@Composable
fun SettingsScreen(
    snackbarHostState: SnackbarHostState,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::exportTo)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importFrom)
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let(viewModel::exportCsvTo)
    }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium) }

        item {
            SoftCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 16.dp)) {
                Overline(stringResource(R.string.settings_general), Modifier.padding(horizontal = 16.dp))
                SettingRow(
                    title = stringResource(R.string.settings_daily_goal),
                    value = formatVolume(state.settings.dailyGoalMl),
                    onClick = { dialog = SettingsDialog.Goal },
                )
                SettingRow(
                    title = stringResource(R.string.settings_glass_size),
                    subtitle = stringResource(
                        R.string.settings_glass_size_desc,
                        formatVolume(state.settings.glassSizeMl),
                        formatVolume(state.settings.halfGlassMl),
                    ),
                    value = formatVolume(state.settings.glassSizeMl),
                    onClick = { dialog = SettingsDialog.Glass },
                )
            }
        }

        item {
            SoftCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 16.dp)) {
                Overline(stringResource(R.string.settings_drink_types), Modifier.padding(horizontal = 16.dp))
                Text(
                    stringResource(R.string.settings_drink_types_desc, HomeUiState.MAX_FAVORITES),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                )
                state.types.forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 16.dp, end = 4.dp),
                    ) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text(type.emoji, style = MaterialTheme.typography.titleMedium) }
                        }
                        Text(
                            type.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f).padding(start = 12.dp),
                        )
                        IconButton(onClick = { viewModel.toggleFavorite(type) }) {
                            Icon(
                                if (type.favorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                                contentDescription = stringResource(R.string.settings_favorite),
                                tint = if (type.favorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { dialog = SettingsDialog.EditType(type) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.edit), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { dialog = SettingsDialog.RemoveType(type) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = stringResource(R.string.remove), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                OutlinedButton(
                    onClick = { dialog = SettingsDialog.AddType },
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.settings_add_drink_type), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        item {
            BackupCard(
                state = state,
                onExport = { exportLauncher.launch(viewModel.exportFileName()) },
                onImport = { dialog = SettingsDialog.ConfirmImport },
                onExportCsv = { csvLauncher.launch(viewModel.csvFileName()) },
            )
        }
    }

    when (val d = dialog) {
        null -> Unit
        SettingsDialog.Goal -> AmountDialog(
            title = stringResource(R.string.settings_daily_goal),
            confirmLabel = stringResource(R.string.save),
            initialMl = state.settings.dailyGoalMl,
            range = 100..10000,
            onDismiss = { dialog = null },
            onConfirm = { viewModel.setDailyGoal(it); dialog = null },
        )
        SettingsDialog.Glass -> AmountDialog(
            title = stringResource(R.string.settings_glass_size),
            confirmLabel = stringResource(R.string.save),
            initialMl = state.settings.glassSizeMl,
            range = 30..2000,
            onDismiss = { dialog = null },
            onConfirm = { viewModel.setGlassSize(it); dialog = null },
        )
        SettingsDialog.AddType -> DrinkTypeDialog(
            title = stringResource(R.string.settings_add_drink_type),
            initialName = "",
            initialEmoji = EmojiChoices.first(),
            onDismiss = { dialog = null },
            onConfirm = { name, emoji -> viewModel.addDrinkType(name, emoji); dialog = null },
        )
        is SettingsDialog.EditType -> DrinkTypeDialog(
            title = stringResource(R.string.settings_edit_drink_type),
            initialName = d.type.name,
            initialEmoji = d.type.emoji,
            onDismiss = { dialog = null },
            onConfirm = { name, emoji -> viewModel.updateDrinkType(d.type.copy(name = name, emoji = emoji)); dialog = null },
        )
        is SettingsDialog.RemoveType -> ConfirmDialog(
            title = stringResource(R.string.settings_remove_type_title, d.type.name),
            message = stringResource(R.string.settings_remove_type_message),
            confirmLabel = stringResource(R.string.remove),
            onDismiss = { dialog = null },
            onConfirm = { viewModel.removeDrinkType(d.type); dialog = null },
        )
        SettingsDialog.ConfirmImport -> ConfirmDialog(
            title = stringResource(R.string.backup_import_confirm_title),
            message = stringResource(R.string.backup_import_confirm_message),
            confirmLabel = stringResource(R.string.backup_import),
            onDismiss = { dialog = null },
            onConfirm = {
                dialog = null
                importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/*"))
            },
        )
    }
}

@Composable
private fun SettingRow(title: String, value: String, onClick: () -> Unit, subtitle: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainer) {
            Text(value, style = MonoStyle, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        }
    }
}

@Composable
private fun BackupCard(
    state: SettingsUiState,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onExportCsv: () -> Unit,
) {
    SoftCard(Modifier.fillMaxWidth()) {
        Overline(stringResource(R.string.backup_title))
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.backup_desc), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        val last = state.settings.lastExportAt
        Text(
            if (last == null) {
                stringResource(R.string.backup_never)
            } else {
                stringResource(
                    R.string.backup_last_export,
                    Instant.ofEpochMilli(last).atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)),
                )
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onExport, enabled = !state.backupBusy, modifier = Modifier.weight(1f).height(48.dp)) {
                Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.backup_export), modifier = Modifier.padding(start = 8.dp))
            }
            OutlinedButton(onClick = onImport, enabled = !state.backupBusy, modifier = Modifier.weight(1f).height(48.dp)) {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.backup_import), modifier = Modifier.padding(start = 8.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(16.dp))
        Overline(stringResource(R.string.csv_title))
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.csv_desc), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onExportCsv, enabled = !state.backupBusy, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.csv_export), modifier = Modifier.padding(start = 8.dp))
        }
        if (state.backupBusy) {
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator(Modifier.size(20.dp).align(Alignment.CenterHorizontally), strokeWidth = 2.dp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DrinkTypeDialog(
    title: String,
    initialName: String,
    initialEmoji: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var emoji by rememberSaveable { mutableStateOf(initialEmoji) }
    val valid = name.isNotBlank() && emoji.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text(stringResource(R.string.settings_type_name)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it.take(8) },
                    label = { Text(stringResource(R.string.settings_type_icon)) },
                    singleLine = true,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EmojiChoices.forEach { choice ->
                        FilterChip(selected = emoji == choice, onClick = { emoji = choice }, label = { Text(choice) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim(), emoji.trim()) }, enabled = valid) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
