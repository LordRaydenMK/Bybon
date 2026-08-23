package dev.sanastasov.bybon.bodyweight.dashboard

import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.bodyweight.domain.BodyWeightRepository
import dev.sanastasov.bybon.ui.stateInWhileInForeground
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val weeklyEntries = listOf(
    WeeklyAverageEntryUi("CW 32", "64.8 kg", "+0.1 vs CW 31"),
    WeeklyAverageEntryUi("CW 31", "64.7 kg", "same as CW 30"),
    WeeklyAverageEntryUi("CW 30", "64.7 kg", "-0.1 vs CW 29")
)

class WeightDashboardViewModel(
    private val repository: BodyWeightRepository,
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
        .stateInWhileInForeground(
            coroutineScope,
            WeightDashboardUiState(false, emptyList(), emptyList())
        )
}

private fun BodyWeightEntry.toUi(): WeightEntryUi =
    WeightEntryUi(date.toString(), "${weight.kilograms} kg")
