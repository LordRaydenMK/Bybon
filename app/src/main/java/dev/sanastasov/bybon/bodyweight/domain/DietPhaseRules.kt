package dev.sanastasov.bybon.bodyweight.domain

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.WeightDelta
import dev.sanastasov.bybon.bodyweight.minusToDelta
import dev.sanastasov.bybon.domain.isoWeekStart
import java.time.LocalDate
import kotlin.math.abs

@Suppress("ReturnCount", "CyclomaticComplexMethod")
fun validateDietPhase(
    kind: DietPhaseKind?,
    startDate: LocalDate,
    startWeight: BodyWeight,
    targetWeight: BodyWeight?,
    durationWeeks: Int?,
): DietPhaseValidation {
    if (kind == null) return DietPhaseValidation.Valid(phase = null)

    val target = targetWeight
        ?: return DietPhaseValidation.Invalid("Enter a target weight")

    if (kind == DietPhaseKind.Maintain) {
        return DietPhaseValidation.Valid(
            DietPhase(kind, startDate, startWeight, target),
        )
    }

    val weeks = durationWeeks
        ?: return DietPhaseValidation.Invalid("Enter 1–$MAX_PHASE_WEEKS weeks")
    if (weeks !in 1..MAX_PHASE_WEEKS) {
        return DietPhaseValidation.Invalid("Enter 1–$MAX_PHASE_WEEKS weeks")
    }

    when (kind) {
        DietPhaseKind.Gain -> if (target <= startWeight) {
            return DietPhaseValidation.Invalid("Gain target must be above current weight")
        }

        DietPhaseKind.Lose -> if (target >= startWeight) {
            return DietPhaseValidation.Invalid("Lose target must be below current weight")
        }

        DietPhaseKind.Maintain -> Unit
    }

    val phase = DietPhase(kind, startDate, startWeight, target, weeks)
    val rate = phase.plannedRatePerWeek()
    val cap = maxWeeklyRate(kind, startWeight)
    if (rate.absolute() > cap) {
        val percent = when (kind) {
            DietPhaseKind.Gain -> MAX_GAIN_PERCENT_PER_WEEK
            DietPhaseKind.Lose -> MAX_LOSE_PERCENT_PER_WEEK
            DietPhaseKind.Maintain -> 0f
        }
        return DietPhaseValidation.Invalid(
            "Too fast: max ${cap.signedKilograms()} kg/week ($percent%). " +
                "Increase weeks or reduce the target.",
        )
    }

    return DietPhaseValidation.Valid(
        phase,
        DietPhasePlanPreview(rate, phase.endExclusive()!!),
    )
}

@Suppress("ReturnCount")
fun effectiveDietPhase(
    openRecord: DietPhaseRecord?,
    today: LocalDate,
    lastOfficialAverage: BodyWeight?,
): EffectiveDietPhase {
    val phase = openRecord?.phase ?: return EffectiveDietPhase.Off
    if (!phase.isExpired(today)) return EffectiveDietPhase.On(phase)
    val maintainTarget = lastOfficialAverage ?: phase.targetWeight
    return EffectiveDietPhase.On(
        DietPhase(
            DietPhaseKind.Maintain,
            phase.endExclusive() ?: today.isoWeekStart(),
            maintainTarget,
            maintainTarget,
        ),
    )
}

@Suppress("ReturnCount")
fun isOnTrack(
    phase: DietPhase,
    currentAverage: BodyWeight,
    lastKnownWeekAverage: BodyWeight?,
): Boolean? = when (phase.kind) {
    DietPhaseKind.Maintain -> {
        currentAverage.minusToDelta(phase.targetWeight).absolute() <= WeightDelta.WaterNoise
    }

    DietPhaseKind.Gain, DietPhaseKind.Lose -> {
        val lastKnown = lastKnownWeekAverage ?: return null
        val delta = currentAverage.minusToDelta(lastKnown)
        delta in onTrackDeltaRange(phase)
    }
}

fun onTrackDeltaRange(phase: DietPhase): ClosedRange<WeightDelta> {
    val planned = phase.plannedRatePerWeek()
    val cap = maxWeeklyRate(phase.kind, phase.startWeight)
    val twoX = WeightDelta(abs(planned.value) * 2)
    val far = maxOf(WeightDelta.WaterNoise, minOf(twoX, cap))
    return when (phase.kind) {
        DietPhaseKind.Gain -> WeightDelta.Zero..far
        DietPhaseKind.Lose -> -far..WeightDelta.Zero
        DietPhaseKind.Maintain -> -WeightDelta.WaterNoise..WeightDelta.WaterNoise
    }
}
