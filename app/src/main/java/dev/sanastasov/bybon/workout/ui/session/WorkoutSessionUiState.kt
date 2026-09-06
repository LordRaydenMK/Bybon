package dev.sanastasov.bybon.workout.ui.session

import dev.sanastasov.bybon.workout.domain.Exercise
import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import java.time.LocalDateTime
import kotlin.time.Duration

fun WorkoutPlan.toWorkoutSessionUi(): WorkoutSessionUiState = TODO()

enum class ExerciseState {
    NotStated,
    InProgress,
    Completed,
}

data class WorkoutExercise(
    val exercise: Exercise,
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
    val plan: WorkoutPlan,
    val currentPage: Int,
    val state: WorkoutState = WorkoutState.NotStarted,
)