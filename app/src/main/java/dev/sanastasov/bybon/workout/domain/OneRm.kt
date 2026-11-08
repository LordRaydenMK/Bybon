package dev.sanastasov.bybon.workout.domain

import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import java.time.LocalDate
import kotlin.math.roundToInt

/** Brzycki when reps &lt; 10, otherwise Epley. */
fun estimateOneRmKg(weightKg: Float, reps: Int): Float = if (reps < 10) {
    weightKg * 36f / (37 - reps)
} else {
    weightKg * (1 + reps / 30f)
}

fun estimatedOneRm(weight: Weight, reps: Int): Weight =
    Weight.kilograms(estimateOneRmKg(weight.kilogramsValue, reps))

fun estimatedSetOneRm(
    equipment: Equipment?,
    loggedWeight: Weight?,
    reps: Int,
    bodyWeight: Weight?,
    missingWeightIsZeroAssist: Boolean,
): Weight? {
    val load = effectiveLoadForOneRm(
        equipment,
        loggedWeight,
        bodyWeight,
        missingWeightIsZeroAssist,
    ) ?: return null
    return estimatedOneRm(load, reps)
}

fun effectiveLoadForOneRm(
    equipment: Equipment?,
    loggedWeight: Weight?,
    bodyWeight: Weight?,
    missingWeightIsZeroAssist: Boolean,
): Weight? = when {
    equipment != Equipment.AssistedBodyWeight -> loggedWeight
    bodyWeight == null -> null
    loggedWeight != null -> bodyWeight.minusOrNull(loggedWeight)
    missingWeightIsZeroAssist -> bodyWeight
    else -> null
}

fun sevenDayBodyWeightAverage(entries: List<BodyWeightEntry>, on: LocalDate): Weight? {
    val start = on.minusDays(6)
    val window = entries.filter { it.date in start..on }
    if (window.size < 3) return null
    val meanHundredths = window.map { it.weight.value }.average()
    val rounded = meanHundredths.roundToInt()
    return if (rounded > 0) Weight.hundredths(rounded) else null
}
