package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime
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

suspend fun WorkoutsRepository.completeInProgressSet(
    now: LocalDateTime = LocalDateTime.now(),
): WorkoutSession? {
    val session = activeSession().first()
    val target = session?.inProgressSetRef()
    if (session == null || target == null) return session
    val exercise = session.exercises.first { it.id == target.exerciseId }
    val updated = session.completeSet(exercise, target.index, target.isWarmup, now)
    updateWorkout(updated)
    return updated
}

suspend fun WorkoutsRepository.notificationSummarySessionId(
    planId: WorkoutPlanId,
): WorkoutSessionId? {
    val sessions = workoutSessions().first().filter { it.planId == planId }
    if (sessions.any { it.state !is WorkoutState.Completed }) return null
    return sessions
        .filter { it.state is WorkoutState.Completed }
        .maxByOrNull { it.startedAt }
        ?.id
}
