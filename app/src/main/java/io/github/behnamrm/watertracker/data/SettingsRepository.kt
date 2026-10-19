package io.github.behnamrm.watertracker.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class AppSettings(
    val dailyGoalMl: Int = DEFAULT_GOAL_ML,
    val glassSizeMl: Int = DEFAULT_GLASS_ML,
    val lastExportAt: Long? = null,
) {
    val halfGlassMl: Int get() = glassSizeMl / 2

    companion object {
        const val DEFAULT_GOAL_ML = 2000
        const val DEFAULT_GLASS_ML = 250
    }
}

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val dailyGoal = intPreferencesKey("daily_goal_ml")
        val glassSize = intPreferencesKey("glass_size_ml")
        val lastExportAt = longPreferencesKey("last_export_at")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setDailyGoal(ml: Int) = edit { it[Keys.dailyGoal] = ml }

    suspend fun setGlassSize(ml: Int) = edit { it[Keys.glassSize] = ml }

    suspend fun setLastExportAt(millis: Long) = edit { it[Keys.lastExportAt] = millis }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    private fun Preferences.toSettings() = AppSettings(
        dailyGoalMl = this[Keys.dailyGoal] ?: AppSettings.DEFAULT_GOAL_ML,
        glassSizeMl = this[Keys.glassSize] ?: AppSettings.DEFAULT_GLASS_ML,
        lastExportAt = this[Keys.lastExportAt],
    )
}
