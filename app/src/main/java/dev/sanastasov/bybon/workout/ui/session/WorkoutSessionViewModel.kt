package dev.sanastasov.bybon.workout.ui.session

import dev.sanastasov.bybon.ui.stateInWhileInForeground
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import dev.sanastasov.bybon.workout.domain.addExerciseIfAbsent
import dev.sanastasov.bybon.workout.domain.addSet
import dev.sanastasov.bybon.workout.domain.adjustExercise
import dev.sanastasov.bybon.workout.domain.completeSet
import dev.sanastasov.bybon.workout.domain.convertFirstWorkSetToWarmup
import dev.sanastasov.bybon.workout.domain.convertLastWarmupToWorkSet
import dev.sanastasov.bybon.workout.domain.removeExercise
import dev.sanastasov.bybon.workout.domain.removeLastSet
import dev.sanastasov.bybon.workout.domain.requireExercise
import dev.sanastasov.bybon.workout.domain.resetExerciseToPrevious
import dev.sanastasov.bybon.workout.domain.resetSetToPrevious
import dev.sanastasov.bybon.workout.domain.startWorkout
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import dev.sanastasov.bybon.workout.domain.updateReps
import dev.sanastasov.bybon.workout.domain.updateWeight
import dev.sanastasov.bybon.workout.domain.updateWorkout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class WorkoutSessionViewModel(
    val planId: WorkoutPlanId,
    val repository: WorkoutsRepository,
    val coroutineScope: CoroutineScope,
) {
    private val _effects = Channel<WorkoutSessionEffect>(Channel.BUFFERED)
    val effects: Flow<WorkoutSessionEffect> = _effects.receiveAsFlow()

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
            val existing = sessions.firstOrNull { session ->
                session.planId == planId && session.state !is WorkoutState.Completed
            }
            when {
                existing == null -> {
                    val plan = repository.workoutPlans()
                        .first()
                        .first { it.id == planId }
                    val previousSession = sessions
                        .filter { it.planId == planId && it.state is WorkoutState.Completed }
                        .maxByOrNull { it.startedAt }
                    repository.updateWorkout(plan.toWorkoutSession(previousSession))
                }

                existing.state !is WorkoutState.InProgress ||
                    existing.workoutSets.none { it.setState == SetState.InProgress } -> {
                    repository.updateWorkout(existing.startWorkout())
                }
            }
        }
    }

    fun onAction(action: WorkoutSessionAction) {
        coroutineScope.launch {
            when (action) {
                WorkoutSessionAction.OnCancelWorkout -> {
                    val session = uiState.value ?: return@launch
                    repository.deleteWorkout(session.id)
                    _effects.trySend(WorkoutSessionEffect.NavigateBack)
                }

                WorkoutSessionAction.OnAddExercise -> {
                    val session = checkNotNull(uiState.value) { "No active session" }
                    _effects.trySend(
                        WorkoutSessionEffect.OpenExerciseLibrary(session.exercises.map { it.id }),
                    )
                }

                is WorkoutSessionAction.OnExercisePicked -> addPickedExercise(action.exerciseId)

                is WorkoutSessionAction.OnRemoveExercise -> removeSessionExercise(action)

                else -> {
                    val updated = repository.updateWorkout(planId) { session ->
                        reduce(session, action)
                    }
                    if (action is WorkoutSessionAction.OnCompleteSet &&
                        updated.state is WorkoutState.Completed
                    ) {
                        _effects.trySend(WorkoutSessionEffect.NavigateToSummary(updated.id))
                    }
                }
            }
        }
    }

    private suspend fun addPickedExercise(exerciseId: String) {
        val exercise = repository.requireExercise(exerciseId)
        repository.updateWorkout(planId) { it.addExerciseIfAbsent(exercise) }
    }

    private suspend fun removeSessionExercise(action: WorkoutSessionAction.OnRemoveExercise) {
        checkNotNull(uiState.value) { "No active session" }
        val updated = repository.updateWorkout(planId) {
            it.removeExercise(action.exercise.id)
        }
        if (updated.state is WorkoutState.Completed) {
            _effects.trySend(WorkoutSessionEffect.NavigateToSummary(updated.id))
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
