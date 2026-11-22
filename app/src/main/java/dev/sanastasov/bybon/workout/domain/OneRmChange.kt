package dev.sanastasov.bybon.workout.domain

sealed interface OneRmChange {
    data class Improved(
        val how: How,
    ) : OneRmChange

    data object Maintained : OneRmChange

    data class Declined(
        val how: How,
    ) : OneRmChange

    sealed interface How {
        data class OneRm(
            val delta: Weight,
        ) : How

        data class Assistance(
            val delta: Weight,
        ) : How

        data object Reps : How
    }
}

fun oneRmChange(current: Weight, previous: Weight?): OneRmChange? {
    previous ?: return null
    return when {
        current > previous -> OneRmChange.Improved(OneRmChange.How.OneRm(current - previous))
        current < previous -> OneRmChange.Declined(OneRmChange.How.OneRm(previous - current))
        else -> OneRmChange.Maintained
    }
}

fun ExerciseSet.oneRmChange(): OneRmChange? {
    val current = oneRm ?: return null
    return oneRmChange(current, previousOneRm)
}
