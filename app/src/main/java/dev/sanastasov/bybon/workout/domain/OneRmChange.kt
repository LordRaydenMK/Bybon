package dev.sanastasov.bybon.workout.domain

enum class OneRmChange {
    Improved,
    Decreased,
    Unchanged,
}

fun ExerciseSet.oneRmChange(): OneRmChange? {
    val current = oneRm
    val previous = previousOneRm
    return when {
        current == null || previous == null -> null
        current > previous -> OneRmChange.Improved
        current < previous -> OneRmChange.Decreased
        else -> OneRmChange.Unchanged
    }
}
