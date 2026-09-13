package dev.sanastasov.bybon.workout.domain

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

suspend fun WorkoutsRepository.updateWorkout(
    planId: WorkoutPlanId,
    update: (WorkoutSession) -> WorkoutSession,
) {
    val session = workoutSessions().map { allSessions ->
        allSessions.first { it.planId == planId && it.state !is WorkoutState.Completed }
    }.first()
    val updated = update(session)
    updateWorkout(updated)
}
