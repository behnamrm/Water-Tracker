package io.github.behnamrm.watertracker.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.behnamrm.watertracker.data.PeriodStats
import io.github.behnamrm.watertracker.data.SettingsRepository
import io.github.behnamrm.watertracker.data.StatsCalculator
import io.github.behnamrm.watertracker.data.StatsMode
import io.github.behnamrm.watertracker.data.WaterRepository
import io.github.behnamrm.watertracker.util.currentDateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

data class StatsUiState(
    val mode: StatsMode = StatsMode.Daily,
    val page: Int = 0,
    val today: LocalDate = LocalDate.now(),
    val goalMl: Int = 0,
    val stats: PeriodStats? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(
    private val repository: WaterRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val mode = MutableStateFlow(StatsMode.Daily)
    private val page = MutableStateFlow(0)

    val state: StateFlow<StatsUiState> = combine(mode, page, currentDateFlow()) { m, p, today -> Triple(m, p, today) }
        .flatMapLatest { (m, p, today) ->
            val zone = ZoneId.systemDefault()
            val firstDay = WeekFields.of(Locale.getDefault()).firstDayOfWeek
            val range = StatsCalculator.pageRange(m, p, today, firstDay)
            combine(
                repository.observeIntakes(range.startMillis(zone), range.endMillis(zone)),
                settings.settings,
            ) { intakes, s ->
                StatsUiState(
                    mode = m,
                    page = p,
                    today = today,
                    goalMl = s.dailyGoalMl,
                    stats = StatsCalculator.compute(m, range, intakes, s.dailyGoalMl, today, zone),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun setMode(newMode: StatsMode) {
        if (mode.value == newMode) return
        mode.value = newMode
        page.value = 0
    }

    fun older() = page.update { it + 1 }

    fun newer() = page.update { (it - 1).coerceAtLeast(0) }
}
