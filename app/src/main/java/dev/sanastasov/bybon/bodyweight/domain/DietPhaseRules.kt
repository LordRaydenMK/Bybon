package dev.sanastasov.bybon.bodyweight.domain

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.WeightDelta
import dev.sanastasov.bybon.bodyweight.minusToDelta
import dev.sanastasov.bybon.domain.isoWeekStart
import java.time.LocalDate
import kotlin.math.abs

fun validateDietPhase(
    kind: DietPhaseKind?,
    startDate: LocalDate,
    startWeight: BodyWeight,
    targetWeight: BodyWeight?,
    durationWeeks: Int?,
): DietPhaseValidation = when (kind) {
    null -> DietPhaseValidation.Valid(phase = null)
    DietPhaseKind.Maintain -> validateMaintain(startDate, startWeight, targetWeight)
    DietPhaseKind.Gain -> validateGain(startDate, startWeight, targetWeight, durationWeeks)
    DietPhaseKind.Lose -> validateLose(startDate, startWeight, targetWeight, durationWeeks)
}

private fun validateMaintain(
    startDate: LocalDate,
    startWeight: BodyWeight,
    targetWeight: BodyWeight?,
): DietPhaseValidation = when (targetWeight) {
    null -> DietPhaseValidation.Invalid("Enter a target weight")
    else -> DietPhaseValidation.Valid(DietPhase.Maintain(startDate, startWeight, targetWeight))
}

private fun validateGain(
    startDate: LocalDate,
    startWeight: BodyWeight,
    targetWeight: BodyWeight?,
    durationWeeks: Int?,
): DietPhaseValidation = when {
    targetWeight == null -> DietPhaseValidation.Invalid("Enter a target weight")

    invalidWeeks(durationWeeks) -> weeksError()

    targetWeight <= startWeight ->
        DietPhaseValidation.Invalid("Gain target must be above current weight")

    else -> validateChangingRate(
        DietPhase.Gain(startDate, startWeight, targetWeight, checkNotNull(durationWeeks)),
        MAX_GAIN_PERCENT_PER_WEEK,
    )
}

private fun validateLose(
    startDate: LocalDate,
    startWeight: BodyWeight,
    targetWeight: BodyWeight?,
    durationWeeks: Int?,
): DietPhaseValidation = when {
    targetWeight == null -> DietPhaseValidation.Invalid("Enter a target weight")

    invalidWeeks(durationWeeks) -> weeksError()

    targetWeight >= startWeight ->
        DietPhaseValidation.Invalid("Lose target must be below current weight")

    else -> validateChangingRate(
        DietPhase.Lose(startDate, startWeight, targetWeight, checkNotNull(durationWeeks)),
        MAX_LOSE_PERCENT_PER_WEEK,
    )
}

private fun invalidWeeks(weeks: Int?): Boolean = weeks == null || weeks !in 1..MAX_PHASE_WEEKS

private fun weeksError(): DietPhaseValidation =
    DietPhaseValidation.Invalid("Enter 1–$MAX_PHASE_WEEKS weeks")

private fun validateChangingRate(phase: DietPhase, percentCap: Float): DietPhaseValidation {
    val rate = phase.plannedRatePerWeek()
    val cap = phase.maxWeeklyRate()
    return if (rate.absolute() > cap) {
        DietPhaseValidation.Invalid(
            "Too fast: max ${cap.signedKilograms()} kg/week ($percentCap%). " +
                "Increase weeks or reduce the target.",
        )
    } else {
        DietPhaseValidation.Valid(
            phase,
            DietPhasePlanPreview(rate, checkNotNull(phase.endExclusive())),
        )
    }
}

fun effectiveDietPhase(
    openRecord: DietPhaseRecord?,
    today: LocalDate,
    lastOfficialAverage: BodyWeight?,
): EffectiveDietPhase = when {
    openRecord == null -> EffectiveDietPhase.Off

    !openRecord.phase.isExpired(today) -> EffectiveDietPhase.On(openRecord.phase)

    else -> {
        val maintainTarget = lastOfficialAverage ?: openRecord.phase.targetWeight
        EffectiveDietPhase.On(
            DietPhase.Maintain(
                openRecord.phase.endExclusive() ?: today.isoWeekStart(),
                maintainTarget,
                maintainTarget,
            ),
        )
    }
}

fun isOnTrack(
    phase: DietPhase,
    currentAverage: BodyWeight,
    lastKnownWeekAverage: BodyWeight?,
): Boolean? = when (phase) {
    is DietPhase.Maintain -> {
        currentAverage.minusToDelta(phase.targetWeight).absolute() <= WeightDelta.WaterNoise
    }

    is DietPhase.Gain, is DietPhase.Lose -> lastKnownWeekAverage?.let { lastKnown ->
        currentAverage.minusToDelta(lastKnown) in onTrackDeltaRange(phase)
    }
}

fun onTrackDeltaRange(phase: DietPhase): ClosedRange<WeightDelta> {
    val planned = phase.plannedRatePerWeek()
    val cap = phase.maxWeeklyRate()
    val twoX = WeightDelta(abs(planned.value) * 2)
    val far = maxOf(WeightDelta.WaterNoise, minOf(twoX, cap))
    return when (phase) {
        is DietPhase.Gain -> WeightDelta.Zero..far
        is DietPhase.Lose -> -far..WeightDelta.Zero
        is DietPhase.Maintain -> -WeightDelta.WaterNoise..WeightDelta.WaterNoise
    }
}
