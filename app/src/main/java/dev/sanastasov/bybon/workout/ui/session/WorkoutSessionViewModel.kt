package dev.sanastasov.bybon.workout.ui.session

import dev.sanastasov.bybon.ui.stateInWhileInForeground
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutSessionAction
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import dev.sanastasov.bybon.workout.domain.addSet
import dev.sanastasov.bybon.workout.domain.completeSet
import dev.sanastasov.bybon.workout.domain.removeLastSet
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import dev.sanastasov.bybon.workout.domain.updateReps
import dev.sanastasov.bybon.workout.domain.updateWeight
import dev.sanastasov.bybon.workout.domain.updateWorkout
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
        repository.workoutSessions()
            .map { sessions ->
                sessions.firstOrNull { session ->
                    session.planId == planId && session.state !is WorkoutState.Completed
                }
            }
            .distinctUntilChanged()
            .stateInWhileInForeground(coroutineScope, null)

    init {
        coroutineScope.launch {
            val sessions = repository.workoutSessions().first()
            val inProgress = sessions.firstOrNull { session ->
                session.planId == planId &&
                    session.workoutSets.any { it.setState == SetState.InProgress }
            }
            if (inProgress == null) {
                val plan = repository.workoutPlans().first().first { it.id == planId }
                val previousSession = sessions
                    .filter { it.planId == planId && it.state is WorkoutState.Completed }
                    .maxByOrNull { (it.state as WorkoutState.Completed).startedAt }
                repository.updateWorkout(plan.toWorkoutSession(previousSession))
            }
        }
    }

    fun onAction(action: WorkoutSessionAction) {
        when (action) {
            is WorkoutSessionAction.OnCompleteSet -> coroutineScope.launch {
                repository.updateWorkout(planId) { session ->
                    session.completeSet(action.exercise, action.index)
                }
            }

            is WorkoutSessionAction.OnWeightUpdated -> coroutineScope.launch {
                repository.updateWorkout(planId) { session ->
                    action.newWeight.toFloatOrNull()?.let { weight ->
                        session.updateWeight(
                            action.exercise,
                            action.index,
                            Weight.kilograms(weight),
                        )
                    } ?: session
                }
            }

            is WorkoutSessionAction.OnRepsUpdated -> coroutineScope.launch {
                repository.updateWorkout(planId) { session ->
                    action.newReps.toIntOrNull()?.let { reps ->
                        session.updateReps(
                            action.exercise,
                            action.index,
                            reps,
                        )
                    } ?: session
                }
            }

            is WorkoutSessionAction.OnAddSet -> coroutineScope.launch {
                repository.updateWorkout(planId) { session ->
                    session.addSet(action.exercise)
                }
            }

            is WorkoutSessionAction.RemoveLastSet -> coroutineScope.launch {
                repository.updateWorkout(planId) { session ->
                    session.removeLastSet(action.exercise)
                }
            }
        }
    }
}
