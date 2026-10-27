package dev.sanastasov.bybon.workout.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

suspend fun WorkoutsRepository.requireExercise(exerciseId: String): ExerciseDefinition {
    val exercise = exercises().first().firstOrNull { it.id == exerciseId }
    return checkNotNull(exercise) { "Exercise $exerciseId is not in the repository" }
}

suspend fun WorkoutsRepository.updateWorkout(
    planId: WorkoutPlanId,
    update: (WorkoutSession) -> WorkoutSession,
): WorkoutSession {
    val session = workoutSessions()
        .mapNotNull { allSessions ->
            allSessions.firstOrNull { it.planId == planId && it.state !is WorkoutState.Completed }
        }
        .first()
    val updated = update(session)
    updateWorkout(updated)
    return updated
}

fun WorkoutsRepository.activeSession(): Flow<WorkoutSession?> =
    workoutSessions().map { allSessions ->
        allSessions.firstOrNull { it.state == WorkoutState.InProgress }
    }
