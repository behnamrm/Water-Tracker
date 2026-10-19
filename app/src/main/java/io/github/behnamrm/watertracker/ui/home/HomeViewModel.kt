package io.github.behnamrm.watertracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.behnamrm.watertracker.data.AppSettings
import io.github.behnamrm.watertracker.data.DrinkType
import io.github.behnamrm.watertracker.data.IntakeWithType
import io.github.behnamrm.watertracker.data.SettingsRepository
import io.github.behnamrm.watertracker.data.StatsCalculator
import io.github.behnamrm.watertracker.data.WaterRepository
import io.github.behnamrm.watertracker.util.currentDateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

data class DayTotal(val date: LocalDate, val totalMl: Int)

data class TypeTotal(val name: String, val emoji: String, val totalMl: Int)

data class HomeUiState(
    val today: LocalDate = LocalDate.now(),
    val totalMl: Int = 0,
    val goalMl: Int = AppSettings.DEFAULT_GOAL_ML,
    val glassMl: Int = AppSettings.DEFAULT_GLASS_ML,
    val types: List<DrinkType> = emptyList(),
    val selectedType: DrinkType? = null,
    val entries: List<IntakeWithType> = emptyList(),
    /** The seven days of the current week, in order. */
    val week: List<DayTotal> = emptyList(),
    /** Up to three rows for the "by drink" card: today's biggest types, padded with favorites. */
    val todayByType: List<TypeTotal> = emptyList(),
) {
    /** Up to three favorite types, shown as one-tap buttons next to the dropdown. */
    val favorites: List<DrinkType> get() = types.filter { it.favorite }.take(MAX_FAVORITES)
    val progress: Float get() = if (goalMl > 0) totalMl.toFloat() / goalMl else 0f

    /** Average per day over the days of this week up to today. */
    val weekAverageMl: Int
        get() {
            val elapsed = week.filter { !it.date.isAfter(today) }
            return if (elapsed.isEmpty()) 0 else elapsed.sumOf { it.totalMl } / elapsed.size
        }

    companion object {
        const val MAX_FAVORITES = 3
    }
}

sealed interface HomeEvent {
    data class Added(val intakeId: Long, val amountMl: Int, val typeName: String, val timestamp: Long) : HomeEvent
    data class Deleted(val entry: IntakeWithType) : HomeEvent
}

private data class WeekData(val today: LocalDate, val days: List<LocalDate>, val intakes: List<IntakeWithType>)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: WaterRepository,
    settings: SettingsRepository,
) : ViewModel() {

    private val selectedTypeId = MutableStateFlow<Long?>(null)
    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val weekData = currentDateFlow().flatMapLatest { today ->
        val zone = ZoneId.systemDefault()
        val start = StatsCalculator.weekStart(today, WeekFields.of(Locale.getDefault()).firstDayOfWeek)
        val days = (0L until 7L).map { start.plusDays(it) }
        repository.observeIntakes(
            start.atStartOfDay(zone).toInstant().toEpochMilli(),
            start.plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli(),
        ).map { WeekData(today, days, it) }
    }

    val state: StateFlow<HomeUiState> = combine(
        weekData,
        repository.drinkTypes,
        settings.settings,
        selectedTypeId,
    ) { week, types, s, selectedId ->
        val zone = ZoneId.systemDefault()
        val byDay = week.intakes.groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
        val todayEntries = byDay[week.today].orEmpty()
        HomeUiState(
            today = week.today,
            totalMl = todayEntries.sumOf { it.amountMl },
            goalMl = s.dailyGoalMl,
            glassMl = s.glassSizeMl,
            types = types,
            selectedType = types.firstOrNull { it.id == selectedId } ?: types.firstOrNull(),
            entries = todayEntries,
            week = week.days.map { day -> DayTotal(day, byDay[day].orEmpty().sumOf { it.amountMl }) },
            todayByType = todayByType(todayEntries, types),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun todayByType(entries: List<IntakeWithType>, types: List<DrinkType>): List<TypeTotal> {
        val drunk = entries.groupBy { it.drinkTypeId }
            .map { (_, list) -> TypeTotal(list.first().typeName, list.first().typeEmoji, list.sumOf { it.amountMl }) }
            .sortedByDescending { it.totalMl }
        val padding = types.filter { it.favorite }
            .filter { fav -> drunk.none { it.name == fav.name } }
            .map { TypeTotal(it.name, it.emoji, 0) }
        return (drunk + padding).take(3)
    }

    fun selectType(id: Long) {
        selectedTypeId.value = id
    }

    /** Logs a drink of the selected type at [timestamp], or now when it is null. */
    fun add(amountMl: Int, timestamp: Long? = null) {
        val type = state.value.selectedType ?: return
        val time = timestamp ?: System.currentTimeMillis()
        viewModelScope.launch {
            val id = repository.addIntake(type.id, amountMl, time)
            _events.send(HomeEvent.Added(id, amountMl, type.name, time))
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
