package dev.sanastasov.bybon.workout.ui.session

import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.ExerciseSet
import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import java.time.LocalDateTime
import kotlin.time.Duration

fun WorkoutPlan.toWorkoutSessionUi(): WorkoutSessionUiState =
    WorkoutSessionUiState(
        id,
        name,
        description,
        exercises = sets.flatMap { planedSet ->
            (1..planedSet.sets).map {
                planedSet.exercise to planedSet.repRange
            }
        }.mapIndexed { index, (exerciseDefinition, repRange) ->
            WorkoutExercise(
                exerciseDefinition,
                repRange,
                50,
                10,
                if (index == 0) ExerciseState.InProgress else ExerciseState.NotStated
            )
        }.groupBy { ExerciseSet(it.exerciseDefinition, it.repRange) }
    )

enum class ExerciseState {
    NotStated,
    InProgress,
    Completed,
}

data class WorkoutExercise(
    val exerciseDefinition: ExerciseDefinition,
    val repRange: IntRange,
    val weight: Int,
    val reps: Int,
    val state: ExerciseState,
)

sealed class WorkoutState {
    data object NotStarted : WorkoutState()
    data class InProgress(val startedAt: LocalDateTime) : WorkoutState()
    data class Completed(val startedAt: LocalDateTime, val duration: Duration) : WorkoutState()
}

data class WorkoutSessionUiState(
    val planId: WorkoutPlanId,
    val planName: String,
    val planDescription: String?,
    val exercises: Map<ExerciseSet, List<WorkoutExercise>>,
    val currentPage: Int = 0,
    val state: WorkoutState = WorkoutState.NotStarted,
)

fun WorkoutSessionUiState.completeExercise(): WorkoutSessionUiState {
    val exercisesList = exercises.values.flatten()
    val inProgressIndex = exercisesList.indexOfLast { it.state == ExerciseState.InProgress }

    val updated = exercisesList.mapIndexed { index, exercise ->
        when (index) {
            inProgressIndex -> exercise.copy(state = ExerciseState.Completed)
            inProgressIndex + 1 -> exercise.copy(state = ExerciseState.InProgress)
            else -> exercise
        }
    }
    return copy(exercises = updated.groupBy { ExerciseSet(it.exerciseDefinition, it.repRange) })
}

sealed class WorkoutSessionAction {
    data object NoOp : WorkoutSessionAction()
    data class OnCompleteSet(val exercise: WorkoutExercise) : WorkoutSessionAction()
}
