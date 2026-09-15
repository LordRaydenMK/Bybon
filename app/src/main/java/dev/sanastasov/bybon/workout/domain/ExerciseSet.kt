package dev.sanastasov.bybon.workout.domain

data class PreviousSetPerformance(
    val weight: Weight,
    val reps: Int,
) {
    val oneRm: Weight?
        get() = oneRmOrNull(weight, reps)
}

data class ExerciseSet(
    val exerciseDefinition: ExerciseDefinition,
    val weight: Weight,
    val reps: Int,
    val setState: SetState,
    val previous: PreviousSetPerformance? = null,
) {
    val oneRm: Weight?
        get() = oneRmOrNull(weight, reps)
}
