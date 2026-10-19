package io.github.behnamrm.watertracker.ui.settings

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.behnamrm.watertracker.R
import io.github.behnamrm.watertracker.data.AppSettings
import io.github.behnamrm.watertracker.data.DrinkType
import io.github.behnamrm.watertracker.data.SettingsRepository
import io.github.behnamrm.watertracker.data.WaterRepository
import io.github.behnamrm.watertracker.drive.AutoBackupWorker
import io.github.behnamrm.watertracker.drive.DriveAuthorizer
import io.github.behnamrm.watertracker.drive.DriveBackupManager
import io.github.behnamrm.watertracker.ui.home.HomeUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val types: List<DrinkType> = emptyList(),
    val driveBusy: Boolean = false,
)

enum class DriveAction { Backup, Restore, EnableAutoBackup }

class SettingsViewModel(
    private val app: Application,
    private val repository: WaterRepository,
    private val settings: SettingsRepository,
    private val authorizer: DriveAuthorizer,
    private val backup: DriveBackupManager,
) : ViewModel() {

    private val driveBusy = MutableStateFlow(false)
    private var pendingAction: DriveAction? = null

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    /** Consent screens that the UI must launch; the result goes to [onAuthorizationResult]. */
    private val _authRequests = Channel<PendingIntent>(Channel.BUFFERED)
    val authRequests = _authRequests.receiveAsFlow()

    val state: StateFlow<SettingsUiState> =
        combine(settings.settings, repository.drinkTypes, driveBusy) { s, types, busy ->
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

    fun disableAutoBackup() = viewModelScope.launch {
        settings.setAutoBackup(false)
        AutoBackupWorker.schedule(app, enabled = false)
    }

    fun runDriveAction(action: DriveAction) {
        if (driveBusy.value) return
        pendingAction = action
        driveBusy.value = true
        viewModelScope.launch {
            try {
                val result = authorizer.authorize()
                val intent = result.pendingIntent
                if (result.hasResolution() && intent != null) {
                    _authRequests.send(intent)
                } else {
                    perform(action, result.accessToken)
                }
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    fun onAuthorizationResult(data: Intent?) {
        val action = pendingAction ?: return
        viewModelScope.launch {
            try {
                perform(action, authorizer.resultFromIntent(data).accessToken)
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    fun onAuthorizationCancelled() {
        pendingAction = null
        driveBusy.value = false
        _messages.trySend(app.getString(R.string.drive_cancelled))
    }

    private suspend fun perform(action: DriveAction, token: String?) {
        try {
            if (token == null) {
                _messages.send(app.getString(R.string.drive_cancelled))
                return
            }
            when (action) {
                DriveAction.Backup -> {
                    backup.backup(token)
                    _messages.send(app.getString(R.string.drive_backup_done))
                }
                DriveAction.Restore -> {
                    val restored = backup.restore(token)
                    _messages.send(app.getString(if (restored) R.string.drive_restore_done else R.string.drive_no_backup))
                }
                DriveAction.EnableAutoBackup -> {
                    backup.backup(token)
                    settings.setAutoBackup(true)
                    AutoBackupWorker.schedule(app, enabled = true)
                    _messages.send(app.getString(R.string.drive_backup_done))
                }
            }
        } finally {
            pendingAction = null
            driveBusy.value = false
        }
    }

    private suspend fun fail(e: Exception) {
        pendingAction = null
        driveBusy.value = false
        _messages.send(app.getString(R.string.drive_error, e.message ?: e.javaClass.simpleName))
    }
}
