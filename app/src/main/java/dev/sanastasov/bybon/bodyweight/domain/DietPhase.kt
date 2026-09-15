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

sealed class DietPhase {
    abstract val startDate: LocalDate
    abstract val startWeight: BodyWeight
    abstract val targetWeight: BodyWeight

    data class Maintain(
        override val startDate: LocalDate,
        override val startWeight: BodyWeight,
        override val targetWeight: BodyWeight,
    ) : DietPhase()

    data class Gain(
        override val startDate: LocalDate,
        override val startWeight: BodyWeight,
        override val targetWeight: BodyWeight,
        val durationWeeks: Int,
    ) : DietPhase()

    data class Lose(
        override val startDate: LocalDate,
        override val startWeight: BodyWeight,
        override val targetWeight: BodyWeight,
        val durationWeeks: Int,
    ) : DietPhase()
}

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

val DietPhase.kind: DietPhaseKind
    get() = when (this) {
        is DietPhase.Maintain -> DietPhaseKind.Maintain
        is DietPhase.Gain -> DietPhaseKind.Gain
        is DietPhase.Lose -> DietPhaseKind.Lose
    }

val DietPhase.durationWeeksOrNull: Int?
    get() = when (this) {
        is DietPhase.Maintain -> null
        is DietPhase.Gain -> durationWeeks
        is DietPhase.Lose -> durationWeeks
    }

fun DietPhase.endExclusive(): LocalDate? = when (this) {
    is DietPhase.Maintain -> null
    is DietPhase.Gain -> startDate.plusWeeks(durationWeeks.toLong())
    is DietPhase.Lose -> startDate.plusWeeks(durationWeeks.toLong())
}

fun DietPhase.isExpired(today: LocalDate): Boolean =
    endExclusive()?.let { end -> !today.isBefore(end) } == true

fun DietPhase.plannedRatePerWeek(): WeightDelta {
    val weeks = durationWeeksOrNull ?: return WeightDelta.Zero
    val raw = (targetWeight.value - startWeight.value).toFloat() / weeks
    val roundedTo5 = (raw / 5f).roundToInt() * 5
    return WeightDelta(roundedTo5)
}

fun DietPhase.weeksRemaining(today: LocalDate): Int {
    val duration = durationWeeksOrNull ?: return 0
    val elapsed = ChronoUnit.WEEKS.between(startDate, today.isoWeekStart()).toInt()
    return (duration - elapsed).coerceAtLeast(0)
}

fun DietPhase.maxWeeklyRate(): WeightDelta = when (this) {
    is DietPhase.Maintain -> WeightDelta.WaterNoise
    is DietPhase.Gain -> startWeight.percentOf(MAX_GAIN_PERCENT_PER_WEEK)
    is DietPhase.Lose -> startWeight.percentOf(MAX_LOSE_PERCENT_PER_WEEK)
}

internal operator fun BodyWeight.compareTo(other: BodyWeight): Int = value.compareTo(other.value)
