package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime
import kotlin.time.Duration

data class WorkoutSession(
    val planId: WorkoutPlanId,
    val planName: String,
    val planDescription: String?,
    val exercises: List<WorkoutExercise>,
    val startedAt: LocalDateTime,
    val duration: Duration? = null,
) {
    val id: WorkoutSessionId
        get() = WorkoutSessionId(planId, startedAt)

    val workoutSets: List<ExerciseSet> = exercises.flatMap { it.orderedSets }

    val state: WorkoutState
        get() = when {
            exercises.isEmpty() || exercises.all { it.state == ExerciseState.NotStarted } ->
                WorkoutState.NotStarted

            exercises.all { it.state == ExerciseState.Completed } ->
                WorkoutState.Completed(duration ?: Duration.ZERO)

            else -> WorkoutState.InProgress
        }

    val isResumable: Boolean
        get() = state is WorkoutState.InProgress

    init {
        require(workoutSets.map { it.setState }.filter { it == SetState.InProgress }.size <= 1) {
            "At most 1 set can be in progress. Found ${workoutSets.filter {
                it.setState == SetState.InProgress
            }}"
        }
    }
}
