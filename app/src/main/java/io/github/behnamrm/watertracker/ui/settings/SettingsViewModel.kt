package io.github.behnamrm.watertracker.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.behnamrm.watertracker.R
import io.github.behnamrm.watertracker.data.AppSettings
import io.github.behnamrm.watertracker.data.DrinkType
import io.github.behnamrm.watertracker.data.SettingsRepository
import io.github.behnamrm.watertracker.data.WaterRepository
import io.github.behnamrm.watertracker.ui.home.HomeUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.time.LocalDate

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val types: List<DrinkType> = emptyList(),
    val backupBusy: Boolean = false,
)

class SettingsViewModel(
    private val app: Application,
    private val repository: WaterRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val backupBusy = MutableStateFlow(false)

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    val state: StateFlow<SettingsUiState> =
        combine(settings.settings, repository.drinkTypes, backupBusy) { s, types, busy ->
            SettingsUiState(s, types, busy)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setDailyGoal(ml: Int) = viewModelScope.launch { settings.setDailyGoal(ml) }

    fun setGlassSize(ml: Int) = viewModelScope.launch { settings.setGlassSize(ml) }

    fun addDrinkType(name: String, emoji: String) = viewModelScope.launch { repository.addDrinkType(name, emoji) }

    fun updateDrinkType(type: DrinkType) = viewModelScope.launch { repository.updateDrinkType(type) }

    fun removeDrinkType(type: DrinkType) = viewModelScope.launch { repository.removeDrinkType(type.id) }

    fun toggleFavorite(type: DrinkType) {
        val favorites = state.value.types.count { it.favorite }
        if (!type.favorite && favorites >= HomeUiState.MAX_FAVORITES) {
            _messages.trySend(app.getString(R.string.settings_favorites_full, HomeUiState.MAX_FAVORITES))
            return
        }
        updateDrinkType(type.copy(favorite = !type.favorite))
    }

    /** Suggested name for a new backup file. */
    fun exportFileName() = "WaterTracker-backup-${LocalDate.now()}.json"

    /** Writes a backup to a file the user picked. */
    fun exportTo(uri: Uri) = runBackupTask(R.string.backup_export_done) {
        val json = repository.exportJson()
        withContext(Dispatchers.IO) {
            val stream = app.contentResolver.openOutputStream(uri, "wt") ?: throw FileNotFoundException(uri.toString())
            stream.bufferedWriter().use { it.write(json) }
        }
        settings.setLastExportAt(System.currentTimeMillis())
    }

    /** Suggested name for a CSV data export. */
    fun csvFileName() = "WaterTracker-data-${LocalDate.now()}.csv"

    /** Writes all drinks as a CSV text file the user picked. */
    fun exportCsvTo(uri: Uri) = runBackupTask(R.string.csv_export_done) {
        val csv = repository.exportCsv()
        withContext(Dispatchers.IO) {
            val stream = app.contentResolver.openOutputStream(uri, "wt") ?: throw FileNotFoundException(uri.toString())
            stream.bufferedWriter().use { it.write(csv) }
        }
    }

    /** Replaces all data with the backup in a file the user picked. */
    fun importFrom(uri: Uri) = runBackupTask(R.string.backup_import_done) {
        val json = withContext(Dispatchers.IO) {
            val stream = app.contentResolver.openInputStream(uri) ?: throw FileNotFoundException(uri.toString())
            stream.bufferedReader().use { it.readText() }
        }
        repository.importJson(json)
    }

    private fun runBackupTask(doneMessage: Int, task: suspend () -> Unit) {
        if (backupBusy.value) return
        backupBusy.value = true
        viewModelScope.launch {
            val message = try {
                task()
                app.getString(doneMessage)
            } catch (e: Exception) {
                app.getString(R.string.backup_error, e.message ?: e.javaClass.simpleName)
            } finally {
                backupBusy.value = false
            }
            _messages.send(message)
        }
    }
}
