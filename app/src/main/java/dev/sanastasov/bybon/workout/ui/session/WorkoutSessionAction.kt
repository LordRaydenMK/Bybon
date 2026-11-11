package dev.sanastasov.bybon.workout.ui.session

import dev.sanastasov.bybon.workout.domain.ReorderDirection
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutSessionId
import dev.sanastasov.bybon.workout.domain.moveDirections
import dev.sanastasov.bybon.workout.ui.ExerciseOverflow

sealed class WorkoutSessionAction {
    data class OnCompleteSet(
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

    data object OnCancelWorkout : WorkoutSessionAction()
    data object OnAddExercise : WorkoutSessionAction()
    data class OnExercisePicked(
        val exerciseId: String,
    ) : WorkoutSessionAction()

    data class OnMoveExerciseUp(
        val exercise: WorkoutExercise,
    ) : WorkoutSessionAction()

    data class OnMoveExerciseDown(
        val exercise: WorkoutExercise,
    ) : WorkoutSessionAction()

    data class OnRemoveExercise(
        val exercise: WorkoutExercise,
    ) : WorkoutSessionAction()

    data class OnOpenExerciseDetails(
        val exerciseId: String,
    ) : WorkoutSessionAction()
}

sealed class WorkoutSessionEffect {
    data object NavigateBack : WorkoutSessionEffect()
    data class NavigateToSummary(
        val sessionId: WorkoutSessionId,
    ) : WorkoutSessionEffect()

    data class OpenExerciseLibrary(
        val existingExerciseIds: List<String>,
    ) : WorkoutSessionEffect()

    data class OpenExerciseDetails(
        val exerciseId: String,
    ) : WorkoutSessionEffect()

    data class ScrollToExercise(
        val page: Int,
    ) : WorkoutSessionEffect()

    data object StopService : WorkoutSessionEffect()
}

fun WorkoutSession.exerciseOverflow(
    index: Int,
    onAction: (WorkoutSessionAction) -> Unit,
): ExerciseOverflow? {
    val exercise = exercises.getOrNull(index) ?: return null
    val directions = moveDirections(exercise)
    return if (directions.isNotEmpty()) {
        ExerciseOverflow(
            canMoveUp = ReorderDirection.Up in directions,
            canMoveDown = ReorderDirection.Down in directions,
            onMoveUp = { onAction(WorkoutSessionAction.OnMoveExerciseUp(exercise)) },
            onMoveDown = { onAction(WorkoutSessionAction.OnMoveExerciseDown(exercise)) },
        )
    } else {
        null
    }
}
