package io.github.behnamrm.watertracker.ui.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
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
    data object ConfirmRestore : SettingsDialog
}

@Composable
fun SettingsScreen(
    snackbarHostState: SnackbarHostState,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }

    val authLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onAuthorizationResult(result.data)
        } else {
            viewModel.onAuthorizationCancelled()
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.authRequests.collect { intent ->
            authLauncher.launch(IntentSenderRequest.Builder(intent.intentSender).build())
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        item { SectionHeader(stringResource(R.string.settings_general)) }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_daily_goal)) },
                trailingContent = { Text(formatVolume(state.settings.dailyGoalMl), style = MaterialTheme.typography.titleSmall) },
                modifier = Modifier.clickable { dialog = SettingsDialog.Goal },
            )
        }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_glass_size)) },
                supportingContent = {
                    Text(
                        stringResource(
                            R.string.settings_glass_size_desc,
                            formatVolume(state.settings.glassSizeMl),
                            formatVolume(state.settings.halfGlassMl),
                        ),
                    )
                },
                trailingContent = { Text(formatVolume(state.settings.glassSizeMl), style = MaterialTheme.typography.titleSmall) },
                modifier = Modifier.clickable { dialog = SettingsDialog.Glass },
            )
        }

        item { SectionHeader(stringResource(R.string.settings_drink_types)) }
        item {
            Text(
                stringResource(R.string.settings_drink_types_desc, HomeUiState.MAX_FAVORITES),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        items(state.types, key = { it.id }) { type ->
            ListItem(
                leadingContent = { Text(type.emoji, style = MaterialTheme.typography.titleLarge) },
                headlineContent = { Text(type.name) },
                trailingContent = {
                    Row {
                        IconButton(onClick = { viewModel.toggleFavorite(type) }) {
                            Icon(
                                if (type.favorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                                contentDescription = stringResource(R.string.settings_favorite),
                                tint = if (type.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { dialog = SettingsDialog.EditType(type) }) {
                            Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.edit))
                        }
                        IconButton(onClick = { dialog = SettingsDialog.RemoveType(type) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = stringResource(R.string.remove))
                        }
                    }
                },
            )
        }
        item {
            TextButton(onClick = { dialog = SettingsDialog.AddType }, modifier = Modifier.padding(horizontal = 8.dp)) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text(stringResource(R.string.settings_add_drink_type), modifier = Modifier.padding(start = 8.dp))
            }
        }

        item { SectionHeader(stringResource(R.string.drive_title)) }
        item {
            DriveCard(
                state = state,
                onBackup = { viewModel.runDriveAction(DriveAction.Backup) },
                onRestore = { dialog = SettingsDialog.ConfirmRestore },
                onAutoBackupChange = { enabled ->
                    if (enabled) viewModel.runDriveAction(DriveAction.EnableAutoBackup) else viewModel.disableAutoBackup()
                },
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
        SettingsDialog.ConfirmRestore -> ConfirmDialog(
            title = stringResource(R.string.drive_restore_confirm_title),
            message = stringResource(R.string.drive_restore_confirm_message),
            confirmLabel = stringResource(R.string.drive_restore),
            onDismiss = { dialog = null },
            onConfirm = { viewModel.runDriveAction(DriveAction.Restore); dialog = null },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Column(Modifier.padding(top = 24.dp)) {
        HorizontalDivider()
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun DriveCard(
    state: SettingsUiState,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onAutoBackupChange: (Boolean) -> Unit,
) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.drive_desc), style = MaterialTheme.typography.bodyMedium)
            val last = state.settings.lastBackupAt
            Text(
                if (last == null) {
                    stringResource(R.string.drive_never)
                } else {
                    stringResource(
                        R.string.drive_last_backup,
                        Instant.ofEpochMilli(last).atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)),
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.driveBusy) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.drive_working), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onBackup, enabled = !state.driveBusy, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.CloudUpload, contentDescription = null)
                    Text(stringResource(R.string.drive_backup_now), modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedButton(onClick = onRestore, enabled = !state.driveBusy, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.CloudDownload, contentDescription = null)
                    Text(stringResource(R.string.drive_restore), modifier = Modifier.padding(start = 8.dp))
                }
            }
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = { Text(stringResource(R.string.drive_auto)) },
                supportingContent = { Text(stringResource(R.string.drive_auto_desc)) },
                trailingContent = {
                    Switch(
                        checked = state.settings.autoBackup,
                        onCheckedChange = onAutoBackupChange,
                        enabled = !state.driveBusy,
                    )
                },
            )
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
