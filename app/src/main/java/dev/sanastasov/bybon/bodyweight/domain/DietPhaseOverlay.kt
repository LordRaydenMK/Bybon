package dev.sanastasov.bybon.bodyweight.domain

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.WeightDelta
import dev.sanastasov.bybon.bodyweight.minus
import dev.sanastasov.bybon.bodyweight.plus
import dev.sanastasov.bybon.domain.weekOfYear
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

fun DietPhase.projectedWeightOn(weekStart: LocalDate): BodyWeight {
    val duration = durationWeeksOrNull ?: return targetWeight
    val elapsed = ChronoUnit.WEEKS.between(startDate, weekStart).toInt()
    return when {
        elapsed <= 0 -> startWeight

        elapsed >= duration -> targetWeight

        else -> {
            val raw = startWeight.value +
                (targetWeight.value - startWeight.value) * elapsed / duration.toFloat()
            BodyWeight((raw / 5f).roundToInt() * 5)
        }
    }
}

fun DietPhase.overlayOn(point: WeeklyTrendPoint): WeeklyTrendPoint {
    val low = targetWeight - WeightDelta.WaterNoise
    val high = targetWeight + WeightDelta.WaterNoise
    val end = endExclusive()
    return when (this) {
        is DietPhase.Maintain -> point.copy(maintainLow = low, maintainHigh = high)

        is DietPhase.Gain, is DietPhase.Lose -> when {
            end != null && !point.weekStart.isBefore(end) ->
                point.copy(maintainLow = low, maintainHigh = high)

            !point.weekStart.isBefore(startDate) ->
                point.copy(projectedWeight = projectedWeightOn(point.weekStart))

            else -> point
        }
    }
}

fun DietPhase.extendTrend(
    trend: List<WeeklyTrendPoint>,
    currentWeekStart: LocalDate,
): List<WeeklyTrendPoint> {
    val lastStart = trend.maxOfOrNull { it.weekStart } ?: currentWeekStart
    val future = (1..CHART_FUTURE_WEEKS).mapNotNull { weeksAhead ->
        val weekStart = currentWeekStart.plusWeeks(weeksAhead.toLong())
        if (weekStart <= lastStart) return@mapNotNull null
        WeeklyTrendPoint(
            weekStart = weekStart,
            weekOfYear = weekStart.weekOfYear,
            isFuture = true,
        )
    }
    return (trend + future).map(::overlayOn)
}
