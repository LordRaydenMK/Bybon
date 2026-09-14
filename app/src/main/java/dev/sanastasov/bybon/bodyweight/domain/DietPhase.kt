package dev.sanastasov.bybon.bodyweight.domain

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.WeightDelta
import dev.sanastasov.bybon.bodyweight.percentOf
import dev.sanastasov.bybon.domain.isoWeekStart
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

const val CHART_FUTURE_WEEKS = 4
const val MAX_PHASE_WEEKS = 30
const val MAX_GAIN_PERCENT_PER_WEEK = 0.5f
const val MAX_LOSE_PERCENT_PER_WEEK = 1.0f

enum class DietPhaseKind {
    Maintain,
    Gain,
    Lose,
}

data class DietPhase(
    val kind: DietPhaseKind,
    val startDate: LocalDate,
    val startWeight: BodyWeight,
    val targetWeight: BodyWeight,
    val durationWeeks: Int? = null,
)

data class DietPhaseRecord(
    val id: Long,
    val phase: DietPhase,
    val endedAt: LocalDate? = null,
)

sealed interface EffectiveDietPhase {
    data object Off : EffectiveDietPhase
    data class On(
        val phase: DietPhase,
    ) : EffectiveDietPhase
}

data class DietPhasePlanPreview(
    val ratePerWeek: WeightDelta,
    val endDate: LocalDate,
)

sealed interface DietPhaseValidation {
    data class Valid(
        val phase: DietPhase?,
        val preview: DietPhasePlanPreview? = null,
    ) : DietPhaseValidation

    data class Invalid(
        val message: String,
    ) : DietPhaseValidation
}

fun DietPhase.endExclusive(): LocalDate? = durationWeeks?.let { startDate.plusWeeks(it.toLong()) }

fun DietPhase.isExpired(today: LocalDate): Boolean {
    val end = endExclusive() ?: return false
    return !today.isBefore(end)
}

fun DietPhase.plannedRatePerWeek(): WeightDelta {
    val weeks = durationWeeks ?: return WeightDelta.Zero
    val raw = (targetWeight.value - startWeight.value).toFloat() / weeks
    val roundedTo5 = (raw / 5f).roundToInt() * 5
    return WeightDelta(roundedTo5)
}

fun DietPhase.weeksRemaining(today: LocalDate): Int {
    val duration = durationWeeks ?: return 0
    val elapsed = ChronoUnit.WEEKS.between(startDate, today.isoWeekStart()).toInt()
    return (duration - elapsed).coerceAtLeast(0)
}

fun maxWeeklyRate(kind: DietPhaseKind, startWeight: BodyWeight): WeightDelta = when (kind) {
    DietPhaseKind.Gain -> startWeight.percentOf(MAX_GAIN_PERCENT_PER_WEEK)
    DietPhaseKind.Lose -> startWeight.percentOf(MAX_LOSE_PERCENT_PER_WEEK)
    DietPhaseKind.Maintain -> WeightDelta.WaterNoise
}

internal operator fun BodyWeight.compareTo(other: BodyWeight): Int = value.compareTo(other.value)
