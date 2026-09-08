package dev.sanastasov.bybon.workout.ui.session

import dev.sanastasov.bybon.ui.stateInWhileInForeground
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutSessionAction
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import dev.sanastasov.bybon.workout.domain.completeSet
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class WorkoutSessionViewModel(
    val planId: WorkoutPlanId,
    val repository: WorkoutsRepository,
    val coroutineScope: CoroutineScope,
) {
    val uiState: StateFlow<WorkoutSession?> =
        repository.workoutSessions().map { it.first() }
            .stateInWhileInForeground(coroutineScope, null)

    init {
        coroutineScope.launch {
            val planFlow = repository.workoutPlans()
                .map { plans -> plans.first { it.id == planId } }
            val session = repository.workoutSessions()
                .first()
                .firstOrNull() ?: planFlow.first().toWorkoutSession()
            repository.updateWorkout(session)
        }
    }

    fun onAction(action: WorkoutSessionAction) {
        coroutineScope.launch {
            val sessions = repository.workoutSessions().first().first()
            repository.updateWorkout(sessions.completeSet())
        }
    }
}