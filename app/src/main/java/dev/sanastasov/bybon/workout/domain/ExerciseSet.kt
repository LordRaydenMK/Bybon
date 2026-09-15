package dev.sanastasov.bybon.workout.domain

data class PreviousSetPerformance(
    val weight: Weight,
    val reps: Int,
) {
    init {
        require(reps > 0) { "reps must be positive" }
    }

    val oneRm: Weight
        get() = estimatedOneRm(weight, reps)
}

data class ExerciseSet(
    val exerciseDefinition: ExerciseDefinition,
    val weight: Weight,
    val reps: Int,
    val setState: SetState,
    val previous: PreviousSetPerformance? = null,
) {
    init {
        require(reps > 0) { "reps must be positive" }
    }

    val oneRm: Weight
        get() = estimatedOneRm(weight, reps)
}
