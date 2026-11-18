package dev.sanastasov.bybon.workout.ui

import dev.sanastasov.bybon.workout.domain.OneRmChange
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.oneRmChange
import kotlin.math.roundToInt

fun oneRmDeltaHundredths(current: Weight, previous: Weight): Int =
    (current.kilogramsValue * 100f).roundToInt() - (previous.kilogramsValue * 100f).roundToInt()

fun formatOneRmChange(current: Weight, previous: Weight): String =
    requireNotNull(oneRmChange(current, previous)).toLabel()

fun OneRmChange.toLabel(): String = when (this) {
    is OneRmChange.Improved -> "↑ ${delta.kilograms} kg"
    is OneRmChange.Decreased -> "↓ ${delta.kilograms} kg"
    OneRmChange.Unchanged -> "No change"
}
