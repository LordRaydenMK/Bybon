package dev.sanastasov.bybon.weight.dashboard

import dev.sanastasov.bybon.weight.BodyWeightEntry
import dev.sanastasov.bybon.weight.WeightRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

val weeklyEntries = listOf(
    WeeklyAverageEntryUi("CW 32", "64.8 kg", "+0.1 vs CW 31"),
    WeeklyAverageEntryUi("CW 31", "64.7 kg", "same as CW 30"),
    WeeklyAverageEntryUi("CW 30", "64.7 kg", "-0.1 vs CW 29")
)

class WeightDashboardViewModel(
    private val repository: WeightRepository,
    private val coroutineScope: CoroutineScope,
) {

    val uiState: StateFlow<WeightDashboardUiState> = repository.entries()
        .map { dailyEntries ->
            WeightDashboardUiState(
                showLogWeight = dailyEntries.firstOrNull { it.date == LocalDate.now() } == null,
                dailyEntries = dailyEntries.map { it.toUi() },
                weeklyEntries,
            )
        }
        .stateIn(
            coroutineScope,
            SharingStarted.WhileSubscribed(5_000),
            WeightDashboardUiState(false, emptyList(), emptyList())
        )
}

private fun BodyWeightEntry.toUi(): WeightEntryUi =
    WeightEntryUi(date.toString(), "${weight.kilograms} kg")
