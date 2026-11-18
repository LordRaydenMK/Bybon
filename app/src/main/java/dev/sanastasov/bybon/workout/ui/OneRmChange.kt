package dev.sanastasov.bybon.workout.ui

import dev.sanastasov.bybon.workout.domain.OneRmChange

fun OneRmChange.toLabel(): String = when (this) {
    is OneRmChange.Improved -> "↑ ${delta.kilograms} kg"
    is OneRmChange.Decreased -> "↓ ${delta.kilograms} kg"
    OneRmChange.Unchanged -> "No change"
}
