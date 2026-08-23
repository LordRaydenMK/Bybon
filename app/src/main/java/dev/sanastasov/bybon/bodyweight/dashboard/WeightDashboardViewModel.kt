package dev.sanastasov.bybon.bodyweight.dashboard

import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.bodyweight.domain.BodyWeightDashboard
import dev.sanastasov.bybon.bodyweight.domain.BodyWeightRepository
import dev.sanastasov.bybon.bodyweight.domain.bodyWeightDashboard
import dev.sanastasov.bybon.domain.weekOfYear
import dev.sanastasov.bybon.ui.stateInWhileInForeground
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class WeightDashboardViewModel(
    private val repository: BodyWeightRepository,
    private val coroutineScope: CoroutineScope,
) {

    private val today = LocalDate.now()

    val uiState: StateFlow<WeightDashboardUiState> = repository.bodyWeightDashboard(today)
        .map { dashboard -> dashboard.toDashboardUi() }
        .stateInWhileInForeground(
            coroutineScope,
            WeightDashboardUiState(false)
        )

    private fun BodyWeightDashboard.toDashboardUi(): WeightDashboardUiState =
        WeightDashboardUiState(
            showLogWeight = thisWeekValues?.firstOrNull { it.date == today } == null,
            comparison = thisWeekAverage?.let { thisWeeksAverage ->
                BodyWeightComparison(
                    today.weekOfYear,
                    "${thisWeeksAverage.kilograms} kg",
                    lastWeekAverage?.let { lastWeekAvg ->
                        PreviousWeekData(
                            LocalDate.now().weekOfYear - 1,
                            "${(thisWeeksAverage - lastWeekAvg).kilograms} kg"
                        )
                    }
                )
            },
            dailyEntries = thisWeekValues?.map { it.toDailyEntryUi() },
            weeklyAverages = previousWeeksAverages?.map { (weekOfYear, entry, delta) ->
                WeeklyAverageEntryUi(
                    "CW $weekOfYear",
                    "${entry.kilograms} kg",
                    delta?.kilograms.let { "$it kg" },
                )
            }
        )

    private fun BodyWeightEntry.toDailyEntryUi(): WeightEntryUi =
        WeightEntryUi(date.toString(), "${weight.kilograms} kg")
}
