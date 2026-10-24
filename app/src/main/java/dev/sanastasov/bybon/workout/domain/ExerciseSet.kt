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
    val completedAt: LocalDateTime? = null,
) {
    init {
        require(reps > 0) { "reps must be positive" }
        require(setState == SetState.Completed || completedAt == null) {
            "completedAt requires a completed set"
        }
    }

    val oneRm: Weight?
        get() = weight?.let { estimatedOneRm(it, reps) }
}
