package dev.sanastasov.bybon.workout.ui

import dev.sanastasov.bybon.workout.domain.OneRmChange

fun OneRmChange.toLabel(): String = when (this) {
    is OneRmChange.Improved -> how.improvedLabel()
    is OneRmChange.Declined -> how.declinedLabel()
    OneRmChange.Maintained -> "No change"
}

private fun OneRmChange.How.improvedLabel(): String = when (this) {
    is OneRmChange.How.OneRm -> "↑ ${delta.kilograms} kg"
    is OneRmChange.How.Assistance -> "↑ ${delta.kilograms} less"
    OneRmChange.How.Reps -> "↑ reps"
}

private fun OneRmChange.How.declinedLabel(): String = when (this) {
    is OneRmChange.How.OneRm -> "↓ ${delta.kilograms} kg"
    is OneRmChange.How.Assistance -> "↓ ${delta.kilograms} more"
    OneRmChange.How.Reps -> "↓ reps"
}
