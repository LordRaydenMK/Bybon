package dev.sanastasov.bybon.workout.ui

import dev.sanastasov.bybon.workout.domain.Weight
import kotlin.math.abs
import kotlin.math.roundToInt

fun oneRmDeltaHundredths(current: Weight, previous: Weight): Int =
    (current.kilogramsValue * 100f).roundToInt() - (previous.kilogramsValue * 100f).roundToInt()

fun formatOneRmChange(current: Weight?, previous: Weight?): String {
    val deltaHundredths = if (current != null && previous != null) {
        oneRmDeltaHundredths(current, previous)
    } else {
        null
    }
    return when {
        deltaHundredths == null -> MISSING_LOAD_LABEL
        deltaHundredths == 0 -> "No change"
        deltaHundredths > 0 -> "↑ ${formatOneRmKg(abs(deltaHundredths) / 100f)} kg"
        else -> "↓ ${formatOneRmKg(abs(deltaHundredths) / 100f)} kg"
    }
}
