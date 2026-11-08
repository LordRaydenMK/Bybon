package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime

data class PreviousSetPerformance(
    val weight: Weight?,
    val reps: Int,
    val bodyWeight: Weight? = null,
) {
    init {
        require(reps > 0) { "reps must be positive" }
    }

    fun oneRm(equipment: Equipment?): Weight? = estimatedSetOneRm(
        equipment = equipment,
        loggedWeight = weight,
        reps = reps,
        bodyWeight = bodyWeight,
        missingWeightIsZeroAssist = true,
    )
}

data class ExerciseSet(
    val exerciseDefinition: ExerciseDefinition,
    val weight: Weight?,
    val reps: Int,
    val setState: SetState,
    val previous: PreviousSetPerformance? = null,
    val bodyWeight: Weight? = null,
) {
    init {
        require(reps > 0) { "reps must be positive" }
    }

    val completedAt: LocalDateTime?
        get() = (setState as? SetState.Completed)?.at

    val oneRm: Weight?
        get() = estimatedSetOneRm(
            equipment = exerciseDefinition.equipment,
            loggedWeight = weight,
            reps = reps,
            bodyWeight = bodyWeight,
            missingWeightIsZeroAssist = setState is SetState.Completed,
        )

    val previousOneRm: Weight?
        get() = previous?.oneRm(exerciseDefinition.equipment)
}
