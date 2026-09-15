package dev.sanastasov.bybon.workout.ui.session

import dev.sanastasov.bybon.workout.domain.WorkoutExercise

sealed class WorkoutSessionAction {
    data class OnCompleteSet(
        val exercise: WorkoutExercise,
        val index: Int,
        val isWarmup: Boolean = false,
    ) : WorkoutSessionAction()

    data class OnUncompleteSet(
        val exercise: WorkoutExercise,
        val index: Int,
        val isWarmup: Boolean = false,
    ) : WorkoutSessionAction()

    data class OnWeightUpdated(
        val newWeight: String,
        val exercise: WorkoutExercise,
        val index: Int,
        val isWarmup: Boolean = false,
    ) : WorkoutSessionAction()

    data class OnRepsUpdated(
        val newReps: String,
        val exercise: WorkoutExercise,
        val index: Int,
        val isWarmup: Boolean = false,
    ) : WorkoutSessionAction()

    data class OnAddSet(
        val exercise: WorkoutExercise,
    ) : WorkoutSessionAction()

    data class RemoveLastSet(
        val exercise: WorkoutExercise,
    ) : WorkoutSessionAction()

    data class OnConvertToWarmup(
        val exercise: WorkoutExercise,
    ) : WorkoutSessionAction()

    data class OnConvertToWorkSet(
        val exercise: WorkoutExercise,
    ) : WorkoutSessionAction()

    data class OnIncreaseExercise(
        val exercise: WorkoutExercise,
    ) : WorkoutSessionAction()

    data class OnDecreaseExercise(
        val exercise: WorkoutExercise,
    ) : WorkoutSessionAction()

    data class OnResetSet(
        val exercise: WorkoutExercise,
        val index: Int,
        val isWarmup: Boolean = false,
    ) : WorkoutSessionAction()

    data class OnResetExercise(
        val exercise: WorkoutExercise,
    ) : WorkoutSessionAction()
}

sealed class WorkoutSessionEffect {
    data class ShowExercise(
        val index: Int,
    ) : WorkoutSessionEffect()
}
