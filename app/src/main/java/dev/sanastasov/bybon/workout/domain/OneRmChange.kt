package dev.sanastasov.bybon.workout.domain

sealed interface OneRmChange {
    sealed interface Gain : OneRmChange

    sealed interface Loss : OneRmChange

    data class Improved(
        val delta: Weight,
    ) : Gain

    data class Decreased(
        val delta: Weight,
    ) : Loss

    data class LessAssistance(
        val delta: Weight,
    ) : Gain

    data class MoreAssistance(
        val delta: Weight,
    ) : Loss

    data object MoreReps : Gain

    data object FewerReps : Loss

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
