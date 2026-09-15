package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime

data class WorkoutSession(
    val planId: WorkoutPlanId,
    val planName: String,
    val planDescription: String?,
    val exercises: List<WorkoutExercise>,
    val startedAt: LocalDateTime,
    val state: WorkoutState = WorkoutState.NotStarted,
) {
    val id: WorkoutSessionId
        get() = WorkoutSessionId(planId, startedAt)

    val workoutSets: List<ExerciseSet> = exercises.flatMap { it.orderedSets }

    init {
        require(workoutSets.map { it.setState }.filter { it == SetState.InProgress }.size <= 1) {
            "At most 1 set can be in progress. Found ${workoutSets.filter {
                it.setState == SetState.InProgress
            }}"
        }
        if (state is WorkoutState.Completed) {
            val incomplete = workoutSets.filter { it.setState != SetState.Completed }
            require(workoutSets.isNotEmpty() && incomplete.isEmpty()) {
                "Completed workout $id has incomplete sets: $incomplete"
            }
        }
    }
}
