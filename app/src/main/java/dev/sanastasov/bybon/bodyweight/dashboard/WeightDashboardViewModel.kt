package dev.sanastasov.bybon.bodyweight.dashboard

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.domain.BodyWeightDashboard
import dev.sanastasov.bybon.bodyweight.domain.BodyWeightRepository
import dev.sanastasov.bybon.bodyweight.domain.DietPhase
import dev.sanastasov.bybon.bodyweight.domain.DietPhaseKind
import dev.sanastasov.bybon.bodyweight.domain.DietPhaseRepository
import dev.sanastasov.bybon.bodyweight.domain.EffectiveDietPhase
import dev.sanastasov.bybon.bodyweight.domain.WeeklyTrendPoint
import dev.sanastasov.bybon.bodyweight.domain.bodyWeightDashboard
import dev.sanastasov.bybon.bodyweight.domain.plannedRatePerWeek
import dev.sanastasov.bybon.bodyweight.domain.weeksRemaining
import dev.sanastasov.bybon.domain.weekOfYear
import dev.sanastasov.bybon.ui.stateInWhileInForeground
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class WeightDashboardViewModel(
    private val repository: BodyWeightRepository,
    private val dietPhaseRepository: DietPhaseRepository,
    private val coroutineScope: CoroutineScope,
    private val today: LocalDate = LocalDate.now(),
) {

    val uiState: StateFlow<WeightDashboardUiState> =
        repository.bodyWeightDashboard(today, dietPhaseRepository.openPhase())
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
            weeklyTrend = weeklyTrend?.toTrendUi(),
            dietPhaseSummary = dietPhaseSummary(),
            onTrack = onTrack == true,
        )

    private fun BodyWeightDashboard.dietPhaseSummary(): DietPhaseSummaryUi? {
        if (currentAverage == null) return null
        return when (val phase = effectivePhase) {
            EffectiveDietPhase.Off -> DietPhaseSummaryUi(
                kindLabel = "None",
                detailLines = listOf("No target"),
                actionLabel = "Set phase",
            )

            is EffectiveDietPhase.On -> phase.phase.toSummaryUi(today)
        }
    }

    private fun List<WeeklyTrendPoint>.toTrendUi(): List<WeeklyTrendPointUi> {
        val firstWeekStart = minOf { it.weekStart }
        return map { point ->
            WeeklyTrendPointUi(
                weekLabel = point.weekOfYear.toString(),
                weekIndex = ChronoUnit.WEEKS.between(firstWeekStart, point.weekStart).toInt(),
                kilograms = point.averageWeight?.kilograms,
                isLastSevenDaysFallback = point.isLastSevenDaysFallback,
                projectedKilograms = point.projectedWeight?.kilograms,
                maintainLowKilograms = point.maintainLow?.kilograms,
                maintainHighKilograms = point.maintainHigh?.kilograms,
                isFuture = point.isFuture,
            )
        }
    }

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
        lastKnownWeekAverage?.let { lastWeekAvg ->
            PreviousWeekData(
                previousWeeksAverages?.firstOrNull()?.weekOfYear ?: (today.weekOfYear - 1),
                "${(average.value - lastWeekAvg.value) / 100f} kg",
            )
        }
}

private fun DietPhase.toSummaryUi(today: LocalDate): DietPhaseSummaryUi {
    val details = buildList {
        add("Target ${targetWeight.kilograms} kg")
        if (kind != DietPhaseKind.Maintain) {
            add(plannedRatePerWeek().formatRate(startWeight))
            add("${weeksRemaining(today)} of $durationWeeks weeks left")
        }
    }
    return DietPhaseSummaryUi(kind.name, details, "Edit")
}
