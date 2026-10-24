package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime

data class PreviousSetPerformance(
    val weight: Weight?,
    val reps: Int,
) {
    init {
        require(reps > 0) { "reps must be positive" }
    }

    val oneRm: Weight?
        get() = weight?.let { estimatedOneRm(it, reps) }
}

data class ExerciseSet(
    val exerciseDefinition: ExerciseDefinition,
    val weight: Weight?,
    val reps: Int,
    val setState: SetState,
    val previous: PreviousSetPerformance? = null,
) {
    init {
        require(reps > 0) { "reps must be positive" }
    }

    val completedAt: LocalDateTime?
        get() = (setState as? SetState.Completed)?.at

    val oneRm: Weight?
        get() = weight?.let { estimatedOneRm(it, reps) }
}
