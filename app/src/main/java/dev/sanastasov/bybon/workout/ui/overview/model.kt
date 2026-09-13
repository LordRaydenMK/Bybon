package dev.sanastasov.bybon.workout.ui.overview

import dev.sanastasov.bybon.workout.domain.WorkoutExercise

sealed class WorkoutOverviewAction {
    data object OnIncreaseWorkout : WorkoutOverviewAction()
    data object OnDecreaseWorkout : WorkoutOverviewAction()
    data class OnIncreaseExercise(
        val exercise: WorkoutExercise
    ) : WorkoutOverviewAction()
    data class OnDecreaseExercise(
        val exercise: WorkoutExercise
    ) : WorkoutOverviewAction()
    data class OnWeightUpdated(
        val newWeight: String,
        val exercise: WorkoutExercise,
        val index: Int,
        val isWarmup: Boolean = false,
    ) : WorkoutOverviewAction()

    data class OnRepsUpdated(
        val newReps: String,
        val exercise: WorkoutExercise,
        val index: Int,
        val isWarmup: Boolean = false,
    ) : WorkoutOverviewAction()

    data class OnAddSet(
        val exercise: WorkoutExercise
    ) : WorkoutOverviewAction()
    data class RemoveLastSet(
        val exercise: WorkoutExercise
    ) : WorkoutOverviewAction()
    data class OnConvertToWarmup(
        val exercise: WorkoutExercise
    ) : WorkoutOverviewAction()
    data class OnConvertToWorkSet(
        val exercise: WorkoutExercise
    ) : WorkoutOverviewAction()
    data object OnResetWorkout : WorkoutOverviewAction()
    data class OnResetExercise(
        val exercise: WorkoutExercise,
    ) : WorkoutOverviewAction()
    data object OnStartWorkout : WorkoutOverviewAction()
}

sealed class WorkoutOverviewEffect {
    data object NavigateToSession : WorkoutOverviewEffect()
}
