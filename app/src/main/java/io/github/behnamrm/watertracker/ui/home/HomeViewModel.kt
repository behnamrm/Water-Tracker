package io.github.behnamrm.watertracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.behnamrm.watertracker.data.AppSettings
import io.github.behnamrm.watertracker.data.DrinkType
import io.github.behnamrm.watertracker.data.IntakeWithType
import io.github.behnamrm.watertracker.data.SettingsRepository
import io.github.behnamrm.watertracker.data.WaterRepository
import io.github.behnamrm.watertracker.util.currentDateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId

data class HomeUiState(
    val totalMl: Int = 0,
    val goalMl: Int = AppSettings.DEFAULT_GOAL_ML,
    val glassMl: Int = AppSettings.DEFAULT_GLASS_ML,
    val types: List<DrinkType> = emptyList(),
    val selectedType: DrinkType? = null,
    val entries: List<IntakeWithType> = emptyList(),
) {
    /** Up to three favorite types, shown as one-tap buttons next to the dropdown. */
    val favorites: List<DrinkType> get() = types.filter { it.favorite }.take(MAX_FAVORITES)
    val progress: Float get() = if (goalMl > 0) totalMl.toFloat() / goalMl else 0f

    companion object {
        const val MAX_FAVORITES = 3
    }
}

sealed interface HomeEvent {
    data class Added(val intakeId: Long, val amountMl: Int, val typeName: String) : HomeEvent
    data class Deleted(val entry: IntakeWithType) : HomeEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: WaterRepository,
    settings: SettingsRepository,
) : ViewModel() {

    private val selectedTypeId = MutableStateFlow<Long?>(null)
    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val todayEntries = currentDateFlow().flatMapLatest { today ->
        val zone = ZoneId.systemDefault()
        repository.observeIntakes(
            today.atStartOfDay(zone).toInstant().toEpochMilli(),
            today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
        )
    }

    val state: StateFlow<HomeUiState> = combine(
        todayEntries,
        repository.drinkTypes,
        settings.settings,
        selectedTypeId,
    ) { entries, types, s, selectedId ->
        HomeUiState(
            totalMl = entries.sumOf { it.amountMl },
            goalMl = s.dailyGoalMl,
            glassMl = s.glassSizeMl,
            types = types,
            selectedType = types.firstOrNull { it.id == selectedId } ?: types.firstOrNull(),
            entries = entries,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun selectType(id: Long) {
        selectedTypeId.value = id
    }

    /** Logs a drink of the selected type; the date and time are taken from the clock. */
    fun add(amountMl: Int) {
        val type = state.value.selectedType ?: return
        viewModelScope.launch {
            val id = repository.addIntake(type.id, amountMl)
            _events.send(HomeEvent.Added(id, amountMl, type.name))
        }
    }

    fun delete(entry: IntakeWithType) {
        viewModelScope.launch {
            repository.deleteIntake(entry.id)
            _events.send(HomeEvent.Deleted(entry))
        }
    }

    fun undo(event: HomeEvent) {
        viewModelScope.launch {
            when (event) {
                is HomeEvent.Added -> repository.deleteIntake(event.intakeId)
                is HomeEvent.Deleted -> repository.restoreIntake(event.entry.toIntake())
            }
        }
    }
}
