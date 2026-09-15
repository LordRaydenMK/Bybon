@file:Suppress("TooManyFunctions")

package dev.sanastasov.bybon.bodyweight.domain

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.WeightDelta
import dev.sanastasov.bybon.bodyweight.percentOf
import dev.sanastasov.bybon.domain.isoWeekStart
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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

    @ConsistentCopyVisibility
    data class Maintain internal constructor(
        override val startDate: LocalDate,
        override val startWeight: BodyWeight,
        override val targetWeight: BodyWeight,
    ) : DietPhase()

    @ConsistentCopyVisibility
    data class Gain internal constructor(
        override val startDate: LocalDate,
        override val startWeight: BodyWeight,
        override val targetWeight: BodyWeight,
        val durationWeeks: Int,
    ) : DietPhase() {
        init {
            requireValidDuration(durationWeeks)
            require(targetWeight > startWeight) {
                "Gain target must be above start weight. Found '$targetWeight' from '$startWeight'"
            }
            requireValidRate(startWeight, targetWeight, durationWeeks, MAX_GAIN_PERCENT_PER_WEEK)
        }
    }

    @ConsistentCopyVisibility
    data class Lose internal constructor(
        override val startDate: LocalDate,
        override val startWeight: BodyWeight,
        override val targetWeight: BodyWeight,
        val durationWeeks: Int,
    ) : DietPhase() {
        init {
            requireValidDuration(durationWeeks)
            require(targetWeight < startWeight) {
                "Lose target must be below start weight. Found '$targetWeight' from '$startWeight'"
            }
            requireValidRate(startWeight, targetWeight, durationWeeks, MAX_LOSE_PERCENT_PER_WEEK)
        }
    }

    companion object {
        fun create(
            kind: DietPhaseKind,
            startDate: LocalDate,
            startWeight: BodyWeight,
            targetWeight: BodyWeight?,
            durationWeeks: Int? = null,
        ): DietPhaseValidation = when (kind) {
            DietPhaseKind.Maintain -> createMaintain(startDate, startWeight, targetWeight)
            DietPhaseKind.Gain -> createGain(startDate, startWeight, targetWeight, durationWeeks)
            DietPhaseKind.Lose -> createLose(startDate, startWeight, targetWeight, durationWeeks)
        }

        private fun createMaintain(
            startDate: LocalDate,
            startWeight: BodyWeight,
            targetWeight: BodyWeight?,
        ): DietPhaseValidation = when (targetWeight) {
            null -> DietPhaseValidation.Invalid("Enter a target weight")
            else -> DietPhaseValidation.Valid(Maintain(startDate, startWeight, targetWeight))
        }

        private fun createGain(
            startDate: LocalDate,
            startWeight: BodyWeight,
            targetWeight: BodyWeight?,
            durationWeeks: Int?,
        ): DietPhaseValidation = when {
            targetWeight == null -> DietPhaseValidation.Invalid("Enter a target weight")

            durationWeeks == null || durationWeeks !in 1..MAX_PHASE_WEEKS ->
                DietPhaseValidation.Invalid("Enter 1–$MAX_PHASE_WEEKS weeks")

            targetWeight <= startWeight ->
                DietPhaseValidation.Invalid("Gain target must be above current weight")

            else -> rateOrValid(
                startWeight,
                targetWeight,
                checkNotNull(durationWeeks),
                MAX_GAIN_PERCENT_PER_WEEK,
            ) { weeks ->
                Gain(startDate, startWeight, targetWeight, weeks)
            }
        }

        private fun createLose(
            startDate: LocalDate,
            startWeight: BodyWeight,
            targetWeight: BodyWeight?,
            durationWeeks: Int?,
        ): DietPhaseValidation = when {
            targetWeight == null -> DietPhaseValidation.Invalid("Enter a target weight")

            durationWeeks == null || durationWeeks !in 1..MAX_PHASE_WEEKS ->
                DietPhaseValidation.Invalid("Enter 1–$MAX_PHASE_WEEKS weeks")

            targetWeight >= startWeight ->
                DietPhaseValidation.Invalid("Lose target must be below current weight")

            else -> rateOrValid(
                startWeight,
                targetWeight,
                checkNotNull(durationWeeks),
                MAX_LOSE_PERCENT_PER_WEEK,
            ) { weeks ->
                Lose(startDate, startWeight, targetWeight, weeks)
            }
        }

        private fun rateOrValid(
            startWeight: BodyWeight,
            targetWeight: BodyWeight,
            weeks: Int,
            percentCap: Float,
            construct: (Int) -> DietPhase,
        ): DietPhaseValidation {
            val rate = weeklyRate(startWeight, targetWeight, weeks)
            val cap = startWeight.percentOf(percentCap)
            return if (rate.absolute() > cap) {
                DietPhaseValidation.Invalid(
                    "Too fast: max ${cap.signedKilograms()} kg/week ($percentCap%). " +
                        "Increase weeks or reduce the target.",
                )
            } else {
                DietPhaseValidation.Valid(construct(weeks))
            }
        }
    }
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

sealed interface DietPhaseValidation {
    data class Valid(
        val phase: DietPhase,
    ) : DietPhaseValidation

    data class Invalid(
        val message: String,
    ) : DietPhaseValidation
}

fun DietPhaseValidation.requireValid(): DietPhase = when (this) {
    is DietPhaseValidation.Valid -> phase
    is DietPhaseValidation.Invalid -> error(message)
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
    return weeklyRate(startWeight, targetWeight, weeks)
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
