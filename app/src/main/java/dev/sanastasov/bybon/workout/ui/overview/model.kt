package dev.sanastasov.bybon.workout.ui.overview

import dev.sanastasov.bybon.domain.toDisplayDate
import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.ExerciseSet
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.ui.ExerciseOverflow
import java.time.LocalDateTime

data class WorkoutOverviewUiState(
    val session: WorkoutSession,
    val previousDate: String? = null,
) {
    val exercises: List<WorkoutExercise> get() = session.exercises
    val workoutSets: List<ExerciseSet> get() = session.workoutSets
}

fun List<WorkoutSession>.latestCompletedFor(planId: WorkoutPlanId): WorkoutSession? =
    filter { it.planId == planId && it.state is WorkoutState.Completed }
        .maxByOrNull { it.startedAt }

fun previousPlanDateLabel(startedAt: LocalDateTime?): String? =
    startedAt?.toLocalDate()?.toDisplayDate()?.let { "Previous: $it" }

sealed class WorkoutOverviewAction {
    data object OnIncreaseWorkout : WorkoutOverviewAction()
    data object OnDecreaseWorkout : WorkoutOverviewAction()
    data class OnIncreaseExercise(
        val exercise: WorkoutExercise,
    ) : WorkoutOverviewAction()
    data class OnDecreaseExercise(
        val exercise: WorkoutExercise,
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
        val exercise: WorkoutExercise,
    ) : WorkoutOverviewAction()
    data class RemoveLastSet(
        val exercise: WorkoutExercise,
    ) : WorkoutOverviewAction()
    data class OnConvertToWarmup(
        val exercise: WorkoutExercise,
    ) : WorkoutOverviewAction()
    data class OnConvertToWorkSet(
        val exercise: WorkoutExercise,
    ) : WorkoutOverviewAction()
    data class OnResetSet(
        val exercise: WorkoutExercise,
        val index: Int,
        val isWarmup: Boolean = false,
    ) : WorkoutOverviewAction()
    data class OnResetExercise(
        val exercise: WorkoutExercise,
    ) : WorkoutOverviewAction()
    data object OnStartWorkout : WorkoutOverviewAction()
    data object OnAddExercise : WorkoutOverviewAction()
    data class OnExercisePicked(
        val exerciseId: String,
    ) : WorkoutOverviewAction()
    data class OnExerciseAdded(
        val exercise: ExerciseDefinition,
        val previous: WorkoutExercise? = null,
    ) : WorkoutOverviewAction()
    data class OnMoveExerciseUp(
        val exercise: WorkoutExercise,
    ) : WorkoutOverviewAction()
    data class OnMoveExerciseDown(
        val exercise: WorkoutExercise,
    ) : WorkoutOverviewAction()
    data class OnRemoveExercise(
        val exercise: WorkoutExercise,
    ) : WorkoutOverviewAction()
    data class OnOpenExerciseDetails(
        val exerciseId: String,
    ) : WorkoutOverviewAction()
}

fun WorkoutSession.exerciseOverflow(
    index: Int,
    onAction: (WorkoutOverviewAction) -> Unit,
): ExerciseOverflow? {
    if (exercises.size <= 1) return null
    val exercise = exercises[index]
    return ExerciseOverflow(
        canMoveUp = index > 0,
        canMoveDown = index < exercises.lastIndex,
        onMoveUp = { onAction(WorkoutOverviewAction.OnMoveExerciseUp(exercise)) },
        onMoveDown = { onAction(WorkoutOverviewAction.OnMoveExerciseDown(exercise)) },
    )
}

sealed class WorkoutOverviewEffect {
    data object NavigateToSession : WorkoutOverviewEffect()
    data class OpenExerciseLibrary(
        val existingExerciseIds: List<String>,
    ) : WorkoutOverviewEffect()
    data class OpenExerciseDetails(
        val exerciseId: String,
    ) : WorkoutOverviewEffect()
}
