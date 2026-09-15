package dev.sanastasov.bybon.bodyweight.domain

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.WeightDelta
import dev.sanastasov.bybon.bodyweight.percentOf
import kotlin.math.roundToInt

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
