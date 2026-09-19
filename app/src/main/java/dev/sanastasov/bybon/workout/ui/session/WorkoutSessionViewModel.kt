package dev.sanastasov.bybon.workout.ui.session

import dev.sanastasov.bybon.ui.stateInWhileInForeground
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import dev.sanastasov.bybon.workout.domain.addSet
import dev.sanastasov.bybon.workout.domain.adjustExercise
import dev.sanastasov.bybon.workout.domain.completeSet
import dev.sanastasov.bybon.workout.domain.convertFirstWorkSetToWarmup
import dev.sanastasov.bybon.workout.domain.convertLastWarmupToWorkSet
import dev.sanastasov.bybon.workout.domain.removeLastSet
import dev.sanastasov.bybon.workout.domain.resetExerciseToPrevious
import dev.sanastasov.bybon.workout.domain.resetSetToPrevious
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import dev.sanastasov.bybon.workout.domain.uncompleteSet
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
                val plan = repository.workoutPlans()
                    .first()
                    .first { it.id == planId }
                val previousSession = sessions
                    .filter { it.planId == planId && it.state is WorkoutState.Completed }
                    .maxByOrNull { it.startedAt }
                repository.updateWorkout(plan.toWorkoutSession(previousSession))
            }
        }
    }

    fun onAction(action: WorkoutSessionAction) {
        coroutineScope.launch {
            repository.updateWorkout(planId) { session ->
                reduce(session, action)
            }
        }
    }

    private fun reduce(session: WorkoutSession, action: WorkoutSessionAction): WorkoutSession =
        reduceEdits(session, action) ?: reduceProgression(session, action)

    private fun reduceEdits(
        session: WorkoutSession,
        action: WorkoutSessionAction,
    ): WorkoutSession? = when (action) {
        is WorkoutSessionAction.OnWeightUpdated ->
            action.newWeight.toFloatOrNull()
                ?.let { Weight.kilogramsOrNull(it) }
                ?.let { weight ->
                    session.updateWeight(
                        action.exercise,
                        action.index,
                        weight,
                        action.isWarmup,
                    )
                } ?: session

        is WorkoutSessionAction.OnRepsUpdated ->
            action.newReps.toIntOrNull()?.takeIf { it > 0 }?.let { reps ->
                session.updateReps(
                    action.exercise,
                    action.index,
                    reps,
                    action.isWarmup,
                )
            } ?: session

        is WorkoutSessionAction.OnAddSet -> session.addSet(action.exercise)

        is WorkoutSessionAction.RemoveLastSet -> session.removeLastSet(action.exercise)

        is WorkoutSessionAction.OnConvertToWarmup ->
            session.convertFirstWorkSetToWarmup(action.exercise)

        is WorkoutSessionAction.OnConvertToWorkSet ->
            session.convertLastWarmupToWorkSet(action.exercise)

        else -> null
    }

    private fun reduceProgression(
        session: WorkoutSession,
        action: WorkoutSessionAction,
    ): WorkoutSession = when (action) {
        is WorkoutSessionAction.OnCompleteSet ->
            session.completeSet(action.exercise, action.index, action.isWarmup)

        is WorkoutSessionAction.OnUncompleteSet ->
            session.uncompleteSet(action.exercise, action.index, action.isWarmup)

        is WorkoutSessionAction.OnIncreaseExercise ->
            session.adjustExercise(action.exercise, increase = true)

        is WorkoutSessionAction.OnDecreaseExercise ->
            session.adjustExercise(action.exercise, increase = false)

        is WorkoutSessionAction.OnResetSet ->
            session.resetSetToPrevious(action.exercise, action.index, action.isWarmup)

        is WorkoutSessionAction.OnResetExercise ->
            session.resetExerciseToPrevious(action.exercise)

        else -> session
    }
}
