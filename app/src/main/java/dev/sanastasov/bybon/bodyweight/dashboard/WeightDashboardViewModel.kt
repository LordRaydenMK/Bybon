package dev.sanastasov.bybon.bodyweight.dashboard

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.domain.BodyWeightDashboard
import dev.sanastasov.bybon.bodyweight.domain.BodyWeightRepository
import dev.sanastasov.bybon.bodyweight.domain.bodyWeightDashboard
import dev.sanastasov.bybon.domain.weekOfYear
import dev.sanastasov.bybon.ui.stateInWhileInForeground
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class WeightDashboardViewModel(
    private val repository: BodyWeightRepository,
    private val coroutineScope: CoroutineScope,
    private val today: LocalDate = LocalDate.now(),
) {

    val uiState: StateFlow<WeightDashboardUiState> = repository.bodyWeightDashboard(today)
        .map { dashboard -> dashboard.toDashboardUi() }
        .stateInWhileInForeground(
            coroutineScope,
            WeightDashboardUiState(),
        )

    private fun BodyWeightDashboard.toDashboardUi(): WeightDashboardUiState =
        WeightDashboardUiState(
            logWeightPrompt = if (thisWeekValues?.any { it.date == today } == true) {
                LogWeightPrompt.Compact
            } else {
                LogWeightPrompt.Prominent
            },
            comparison = heroComparison(),
            dailyEntries = thisWeekValues,
            weeklyAverages = previousWeeksAverages?.map { (weekOfYear, entry, delta) ->
                WeeklyAverageEntryUi(
                    "CW $weekOfYear",
                    "${entry.kilograms} kg",
                    delta?.kilograms?.let { "$it kg" },
                )
            },
        )

    private fun BodyWeightDashboard.heroComparison(): BodyWeightComparison? {
        val (title, average) = when {
            thisWeekAverage != null ->
                "CW ${today.weekOfYear} average" to thisWeekAverage

            lastSevenDaysAverage != null ->
                "Last 7d average" to lastSevenDaysAverage

            else -> return null
        }
        return BodyWeightComparison(
            title,
            "${average.kilograms} kg",
            previousWeekData(average),
        )
    }

    private fun BodyWeightDashboard.previousWeekData(average: BodyWeight): PreviousWeekData? =
        lastWeekAverage?.let { lastWeekAvg ->
            PreviousWeekData(
                today.weekOfYear - 1,
                "${(average.value - lastWeekAvg.value) / 100f} kg",
            )
        }
}
