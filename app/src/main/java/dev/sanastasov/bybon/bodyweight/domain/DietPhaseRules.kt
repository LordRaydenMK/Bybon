package dev.sanastasov.bybon.bodyweight.domain

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.WeightDelta
import dev.sanastasov.bybon.bodyweight.minusToDelta
import dev.sanastasov.bybon.bodyweight.percentOf
import dev.sanastasov.bybon.domain.isoWeekStart
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

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

fun DietPhase.isOnTrack(currentAverage: BodyWeight, lastKnownWeekAverage: BodyWeight?): Boolean? =
    when (this) {
        is DietPhase.Maintain -> {
            currentAverage.minusToDelta(targetWeight).absolute() <= WeightDelta.WaterNoise
        }

        is DietPhase.Gain, is DietPhase.Lose -> lastKnownWeekAverage?.let { lastKnown ->
            currentAverage.minusToDelta(lastKnown) in onTrackDeltaRange()
        }
    }

fun DietPhase.onTrackDeltaRange(): ClosedRange<WeightDelta> {
    val planned = plannedRatePerWeek()
    val cap = maxWeeklyRate()
    val twoX = WeightDelta(abs(planned.value) * 2)
    val far = maxOf(WeightDelta.WaterNoise, minOf(twoX, cap))
    return when (this) {
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
