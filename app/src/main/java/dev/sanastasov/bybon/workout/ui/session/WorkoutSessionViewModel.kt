package dev.sanastasov.bybon.workout.ui.session

import dev.sanastasov.bybon.ui.stateInWhileInForeground
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutSessionAction
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import dev.sanastasov.bybon.workout.domain.completeSet
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import dev.sanastasov.bybon.workout.domain.updateReps
import dev.sanastasov.bybon.workout.domain.updateWeight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
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
            .distinctUntilChanged()
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
        when (action) {
            is WorkoutSessionAction.OnCompleteSet -> {
                coroutineScope.launch {
                    val sessions = repository.workoutSessions().first().first()
                    repository.updateWorkout(sessions.completeSet(action.exercise, action.index))
                }
            }

            is WorkoutSessionAction.OnWeightUpdated -> {
                coroutineScope.launch {
                    val session = repository.workoutSessions().first().first()
                    val updated =
                        session.updateWeight(
                            action.exercise,
                            action.index,
                            Weight.parseString(action.newWeight)
                        )
                    repository.updateWorkout(updated)
                }
            }

            is WorkoutSessionAction.OnRepsUpdated -> {
                coroutineScope.launch {
                    val session = repository.workoutSessions().first().first()
                    val updated =
                        session.updateReps(action.exercise, action.index, action.newReps.toInt())
                    repository.updateWorkout(updated)
                }
            }

        }

    }
}