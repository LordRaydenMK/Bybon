package dev.sanastasov.bybon.bodyweight.domain

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.WeightDelta
import dev.sanastasov.bybon.bodyweight.minusToDelta
import dev.sanastasov.bybon.bodyweight.percentOf
import dev.sanastasov.bybon.domain.isoWeekStart
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

internal fun createMaintain(
    startDate: LocalDate,
    startWeight: BodyWeight,
    targetWeight: BodyWeight?,
): DietPhaseValidation = when (targetWeight) {
    null -> DietPhaseValidation.Invalid("Enter a target weight")
    else -> DietPhaseValidation.Valid(DietPhase.Maintain(startDate, startWeight, targetWeight))
}

internal fun createGain(
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
        DietPhase.Gain(startDate, startWeight, targetWeight, weeks)
    }
}

internal fun createLose(
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
        DietPhase.Lose(startDate, startWeight, targetWeight, weeks)
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
            DietPhase.create(
                DietPhaseKind.Maintain,
                openRecord.phase.endExclusive() ?: today.isoWeekStart(),
                maintainTarget,
                maintainTarget,
            ).requireValid(),
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

internal fun weeklyRate(
    startWeight: BodyWeight,
    targetWeight: BodyWeight,
    weeks: Int,
): WeightDelta {
    val raw = (targetWeight.value - startWeight.value).toFloat() / weeks
    val roundedTo5 = (raw / 5f).roundToInt() * 5
    return WeightDelta(roundedTo5)
}

internal fun requireValidDuration(durationWeeks: Int) {
    require(durationWeeks in 1..MAX_PHASE_WEEKS) {
        "Duration must be 1–$MAX_PHASE_WEEKS weeks. Found '$durationWeeks'"
    }
}

internal fun requireValidRate(
    startWeight: BodyWeight,
    targetWeight: BodyWeight,
    weeks: Int,
    percentCap: Float,
) {
    val rate = weeklyRate(startWeight, targetWeight, weeks)
    val cap = startWeight.percentOf(percentCap)
    require(rate.absolute() <= cap) {
        "Weekly rate ${rate.signedKilograms()} kg exceeds cap ${cap.signedKilograms()} kg ($percentCap%)"
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
