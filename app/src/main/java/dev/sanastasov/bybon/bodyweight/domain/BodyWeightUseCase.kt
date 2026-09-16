package dev.sanastasov.bybon.bodyweight.domain

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.domain.isoWeekStart
import dev.sanastasov.bybon.domain.weekOfYear
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal const val WEEKLY_TREND_WEEKS = 16

data class WeeklyAverageEntry(
    val weekOfYear: Int,
    val averageWeight: BodyWeight,
    val delta: BodyWeight?,
)

data class WeeklyTrendPoint(
    val weekStart: LocalDate,
    val weekOfYear: Int,
    val averageWeight: BodyWeight,
    val isLastSevenDaysFallback: Boolean = false,
)

data class BodyWeightDashboard(
    val thisWeekValues: List<BodyWeightEntry>?,
    val previousWeeksAverages: List<WeeklyAverageEntry>?,
    val lastSevenDaysAverage: BodyWeight? = null,
    val weeklyTrend: List<WeeklyTrendPoint>? = null,
    val lastKnownWeekAverage: BodyWeight? = null,
    val effectivePhase: EffectiveDietPhase = EffectiveDietPhase.Off,
    val onTrack: Boolean? = null,
) {

    val thisWeekAverage: BodyWeight? = thisWeekValues?.averageWeight()?.weight

    val currentAverage: BodyWeight? = thisWeekAverage ?: lastSevenDaysAverage

    val lastWeekAverage: BodyWeight? = lastKnownWeekAverage
}

fun BodyWeightRepository.bodyWeightDashboard(today: LocalDate): Flow<BodyWeightDashboard> =
    combine(entries(), openPhase()) { allEntries, phase ->
        computeBodyWeightDashboard(allEntries, phase, today)
    }

internal fun computeBodyWeightDashboard(
    allEntries: List<BodyWeightEntry>,
    openPhase: DietPhaseRecord?,
    today: LocalDate,
): BodyWeightDashboard {
    val currentWeekStart = today.isoWeekStart()
    val lastSevenDaysStart = today.minusDays(6)
    val windowStart = currentWeekStart.minusWeeks((WEEKLY_TREND_WEEKS - 1).toLong())

    val thisWeekValues = allEntries.filter { entry ->
        entry.date.isoWeekStart() == currentWeekStart
    }

    val lastSevenDaysAverage = allEntries.filter { entry ->
        entry.date in lastSevenDaysStart..today
    }.averageWeight()?.weight

    val entriesByWeekStart = allEntries.filter { entry ->
        entry.date >= windowStart.minusWeeks(1) && entry.date <= today
    }.groupBy { it.date.isoWeekStart() }

    val previousWeeksAverages = previousWeeksAverages(currentWeekStart, entriesByWeekStart)
    val lastKnownWeekAverage = previousWeeksAverages.firstOrNull()?.averageWeight
    val thisWeekAverage = thisWeekValues.averageWeight()?.weight
    val currentAverage = thisWeekAverage ?: lastSevenDaysAverage
    val lastOfficialAverage = thisWeekAverage ?: lastKnownWeekAverage
    val effectivePhase = effectiveDietPhase(openPhase, today, lastOfficialAverage)

    val weeklyTrend = weeklyTrend(
        currentWeekStart,
        windowStart,
        entriesByWeekStart,
        lastSevenDaysAverage,
    )

    val onTrack = when {
        currentAverage == null -> null

        effectivePhase is EffectiveDietPhase.On -> effectivePhase.phase.isOnTrack(
            currentAverage,
            lastKnownWeekAverage,
        )

        else -> null
    }

    return BodyWeightDashboard(
        thisWeekValues.takeIf { it.isNotEmpty() },
        previousWeeksAverages.takeIf { it.isNotEmpty() },
        lastSevenDaysAverage,
        weeklyTrend.takeIf { it.isNotEmpty() },
        lastKnownWeekAverage,
        effectivePhase,
        onTrack,
    )
}

private fun previousWeeksAverages(
    currentWeekStart: LocalDate,
    entriesByWeekStart: Map<LocalDate, List<BodyWeightEntry>>,
): List<WeeklyAverageEntry> {
    return (1 until WEEKLY_TREND_WEEKS).mapNotNull { weeksAgo ->
        val weekStart = currentWeekStart.minusWeeks(weeksAgo.toLong())
        val average = entriesByWeekStart[weekStart]?.averageWeight() ?: return@mapNotNull null
        val previousAverage = entriesByWeekStart[weekStart.minusWeeks(1)]?.averageWeight()?.weight
        WeeklyAverageEntry(
            weekStart.weekOfYear,
            average.weight,
            previousAverage?.let { average.weight - it },
        )
    }
}

private fun weeklyTrend(
    currentWeekStart: LocalDate,
    windowStart: LocalDate,
    entriesByWeekStart: Map<LocalDate, List<BodyWeightEntry>>,
    lastSevenDaysAverage: BodyWeight?,
): List<WeeklyTrendPoint> {
    return (0 until WEEKLY_TREND_WEEKS).mapNotNull { weeksFromStart ->
        val weekStart = windowStart.plusWeeks(weeksFromStart.toLong())
        if (weekStart > currentWeekStart) return@mapNotNull null

        val officialAverage = entriesByWeekStart[weekStart]?.averageWeight()?.weight
        val isCurrentWeek = weekStart == currentWeekStart
        val average = officialAverage ?: lastSevenDaysAverage.takeIf { isCurrentWeek }

        average?.let {
            WeeklyTrendPoint(
                weekStart = weekStart,
                weekOfYear = weekStart.weekOfYear,
                averageWeight = it,
                isLastSevenDaysFallback = isCurrentWeek && officialAverage == null,
            )
        }
    }
}

private fun List<BodyWeightEntry>.averageWeight(): BodyWeightEntry? = when {
    isEmpty() -> null

    size < 3 -> null

    else -> {
        val total = sumOf { it.weight.value }
        val average = total.toFloat() / size
        val roundedTo5 = (average / 5).roundToInt() * 5
        BodyWeightEntry(first().date, BodyWeight(roundedTo5))
    }
}
