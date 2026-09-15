package dev.sanastasov.bybon.bodyweight.domain

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.WeightDelta
import dev.sanastasov.bybon.bodyweight.minus
import dev.sanastasov.bybon.bodyweight.plus
import dev.sanastasov.bybon.domain.weekOfYear
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

fun projectedWeightOn(phase: DietPhase, weekStart: LocalDate): BodyWeight {
    val duration = phase.durationWeeksOrNull ?: return phase.targetWeight
    val elapsed = ChronoUnit.WEEKS.between(phase.startDate, weekStart).toInt()
    return when {
        elapsed <= 0 -> phase.startWeight

        elapsed >= duration -> phase.targetWeight

        else -> {
            val raw = phase.startWeight.value +
                (phase.targetWeight.value - phase.startWeight.value) * elapsed / duration.toFloat()
            BodyWeight((raw / 5f).roundToInt() * 5)
        }
    }
}

fun WeeklyTrendPoint.withOverlay(phase: DietPhase): WeeklyTrendPoint {
    val low = phase.targetWeight - WeightDelta.WaterNoise
    val high = phase.targetWeight + WeightDelta.WaterNoise
    val end = phase.endExclusive()
    return when (phase) {
        is DietPhase.Maintain -> copy(maintainLow = low, maintainHigh = high)

        is DietPhase.Gain, is DietPhase.Lose -> when {
            end != null && !weekStart.isBefore(end) -> copy(maintainLow = low, maintainHigh = high)

            !weekStart.isBefore(phase.startDate) ->
                copy(projectedWeight = projectedWeightOn(phase, weekStart))

            else -> this
        }
    }
}

fun extendTrendForPhase(
    trend: List<WeeklyTrendPoint>,
    currentWeekStart: LocalDate,
    phase: DietPhase,
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
    return (trend + future).map { it.withOverlay(phase) }
}
