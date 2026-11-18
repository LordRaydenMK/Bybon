package dev.sanastasov.bybon.workout.domain

sealed interface OneRmChange {
    data class Improved(
        val delta: Weight,
    ) : OneRmChange

    data class Decreased(
        val delta: Weight,
    ) : OneRmChange

    data object Unchanged : OneRmChange
}

fun oneRmChange(current: Weight, previous: Weight?): OneRmChange? {
    previous ?: return null
    return when {
        current > previous -> OneRmChange.Improved(current - previous)
        current < previous -> OneRmChange.Decreased(previous - current)
        else -> OneRmChange.Unchanged
    }
}

fun ExerciseSet.oneRmChange(): OneRmChange? {
    val current = oneRm ?: return null
    return oneRmChange(current, previousOneRm)
}
