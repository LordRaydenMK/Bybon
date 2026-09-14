package dev.sanastasov.bybon.workout.ui.overview

import dev.sanastasov.bybon.ui.stateInWhileInForeground
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import dev.sanastasov.bybon.workout.domain.addSet
import dev.sanastasov.bybon.workout.domain.adjustAll
import dev.sanastasov.bybon.workout.domain.adjustExercise
import dev.sanastasov.bybon.workout.domain.convertFirstWorkSetToWarmup
import dev.sanastasov.bybon.workout.domain.convertLastWarmupToWorkSet
import dev.sanastasov.bybon.workout.domain.removeLastSet
import dev.sanastasov.bybon.workout.domain.resetExercise
import dev.sanastasov.bybon.workout.domain.resetTo
import dev.sanastasov.bybon.workout.domain.startWorkout
import dev.sanastasov.bybon.workout.domain.toOverviewSession
import dev.sanastasov.bybon.workout.domain.updateReps
import dev.sanastasov.bybon.workout.domain.updateWeight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WorkoutOverviewViewModel(
    val planId: WorkoutPlanId,
    val repository: WorkoutsRepository,
    val coroutineScope: CoroutineScope,
) {
    private val actions = MutableSharedFlow<WorkoutOverviewAction>(extraBufferCapacity = 32)
    private val _effects = Channel<WorkoutOverviewEffect>(Channel.BUFFERED)
    val effects: Flow<WorkoutOverviewEffect> = _effects.receiveAsFlow()

    val uiState: StateFlow<WorkoutSession?> =
        combine(
            repository.workoutPlans(),
            repository.workoutSessions(),
        ) { plans, sessions ->
            val plan = plans.firstOrNull { it.id == planId } ?: return@combine null
            val previousSession = sessions
                .filter { it.planId == planId && it.state is WorkoutState.Completed }
                .maxByOrNull { it.startedAt }
            plan.toOverviewSession(previousSession)
        }
            .distinctUntilChanged()
            .filterNotNull()
            .flatMapLatest { seed ->
                actions.scan(seed) { session, action -> reduce(session, action, seed) }
            }
            .stateInWhileInForeground(coroutineScope, null)

    fun onAction(action: WorkoutOverviewAction) {
        when (action) {
            WorkoutOverviewAction.OnStartWorkout -> coroutineScope.launch {
                val session = uiState.value ?: return@launch
                repository.updateWorkout(session.startWorkout())
                _effects.trySend(WorkoutOverviewEffect.NavigateToSession)
            }

            else -> {
                if (!actions.tryEmit(action)) {
                    coroutineScope.launch { actions.emit(action) }
                }
            }
        }
    }

    private fun reduce(
        session: WorkoutSession,
        action: WorkoutOverviewAction,
        seed: WorkoutSession,
    ): WorkoutSession = reduceProgression(session, action, seed) ?: reduceEdits(session, action)

    private fun reduceProgression(
        session: WorkoutSession,
        action: WorkoutOverviewAction,
        seed: WorkoutSession,
    ): WorkoutSession? = when (action) {
        WorkoutOverviewAction.OnIncreaseWorkout -> session.adjustAll(increase = true)

        WorkoutOverviewAction.OnDecreaseWorkout -> session.adjustAll(increase = false)

        is WorkoutOverviewAction.OnIncreaseExercise ->
            session.adjustExercise(action.exercise, increase = true)

        is WorkoutOverviewAction.OnDecreaseExercise ->
            session.adjustExercise(action.exercise, increase = false)

        WorkoutOverviewAction.OnResetWorkout -> session.resetTo(seed)

        is WorkoutOverviewAction.OnResetExercise -> session.resetExercise(action.exercise, seed)

        else -> null
    }

    private fun reduceEdits(
        session: WorkoutSession,
        action: WorkoutOverviewAction,
    ): WorkoutSession = when (action) {
        is WorkoutOverviewAction.OnWeightUpdated ->
            action.newWeight.toFloatOrNull()?.let { weight ->
                session.updateWeight(
                    action.exercise,
                    action.index,
                    Weight.kilograms(weight),
                    action.isWarmup,
                )
            } ?: session

        is WorkoutOverviewAction.OnRepsUpdated ->
            action.newReps.toIntOrNull()?.let { reps ->
                session.updateReps(action.exercise, action.index, reps, action.isWarmup)
            } ?: session

        is WorkoutOverviewAction.OnAddSet -> session.addSet(action.exercise)

        is WorkoutOverviewAction.RemoveLastSet -> session.removeLastSet(action.exercise)

        is WorkoutOverviewAction.OnConvertToWarmup ->
            session.convertFirstWorkSetToWarmup(action.exercise)

        is WorkoutOverviewAction.OnConvertToWorkSet ->
            session.convertLastWarmupToWorkSet(action.exercise)

        else -> session
    }
}
