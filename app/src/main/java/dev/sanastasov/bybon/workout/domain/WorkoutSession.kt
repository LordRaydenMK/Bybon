package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime
import kotlin.time.Duration

data class WorkoutSession(
    val planId: WorkoutPlanId,
    val planName: String,
    val note: String?,
    val exercises: List<WorkoutExercise>,
    val startedAt: LocalDateTime,
    val duration: Duration? = null,
) {
    val id: WorkoutSessionId
        get() = WorkoutSessionId(planId, startedAt)

    val workoutSets: List<ExerciseSet> = exercises.flatMap { it.orderedSets }

    val state: WorkoutState
        get() = when {
            exercises.all { it.state == ExerciseState.NotStarted } ->
                WorkoutState.NotStarted

            exercises.all { it.state == ExerciseState.Completed } ->
                WorkoutState.Completed(duration ?: Duration.ZERO)

            else -> {
                val inProgress = inProgressSet()
                val timer = restFromLastCompleted()
                val next = firstNotStartedSet()
                when {
                    inProgress != null -> WorkoutState.InProgress(inProgress)
                    timer != null && next != null -> WorkoutState.Resting(timer, next)
                    next != null -> WorkoutState.InProgress(next)
                    else -> WorkoutState.Completed(duration ?: Duration.ZERO)
                }
            }
        }

    val isResumable: Boolean
        get() = state is WorkoutState.InProgress || state is WorkoutState.Resting

    val currentExerciseIndex: Int?
        get() = exercises.indexOfFirst { it.state != ExerciseState.Completed }.takeIf { it >= 0 }

    init {
        require(exercises.isNotEmpty()) { "Session must contain at least one exercise" }
        require(workoutSets.map { it.setState }.filter { it == SetState.InProgress }.size <= 1) {
            "At most 1 set can be in progress. Found ${workoutSets.filter {
                it.setState == SetState.InProgress
            }}"
        }
    }
}
