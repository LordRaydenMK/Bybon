package dev.sanastasov.bybon.workout.ui.overview

import dev.sanastasov.bybon.ui.stateInWhileInForeground
import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import dev.sanastasov.bybon.workout.domain.addExercise
import dev.sanastasov.bybon.workout.domain.addSet
import dev.sanastasov.bybon.workout.domain.adjustAll
import dev.sanastasov.bybon.workout.domain.adjustExercise
import dev.sanastasov.bybon.workout.domain.asOverviewDraft
import dev.sanastasov.bybon.workout.domain.convertFirstWorkSetToWarmup
import dev.sanastasov.bybon.workout.domain.convertLastWarmupToWorkSet
import dev.sanastasov.bybon.workout.domain.latestCompletedExercise
import dev.sanastasov.bybon.workout.domain.moveExerciseDown
import dev.sanastasov.bybon.workout.domain.moveExerciseUp
import dev.sanastasov.bybon.workout.domain.removeExercise
import dev.sanastasov.bybon.workout.domain.removeLastSet
import dev.sanastasov.bybon.workout.domain.removeLastWarmupSet
import dev.sanastasov.bybon.workout.domain.requireExercise
import dev.sanastasov.bybon.workout.domain.resetExerciseToPrevious
import dev.sanastasov.bybon.workout.domain.resetSetToPrevious
import dev.sanastasov.bybon.workout.domain.toOverviewSession
import dev.sanastasov.bybon.workout.domain.updateReps
import dev.sanastasov.bybon.workout.domain.updateWeightFromField
import dev.sanastasov.bybon.workout.domain.withPreviousFromHistory
import dev.sanastasov.bybon.workout.ui.session.StartMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class WorkoutOverviewViewModel(
    val planId: WorkoutPlanId,
    val repository: WorkoutsRepository,
    val coroutineScope: CoroutineScope,
) {
    private val actions = Channel<WorkoutOverviewAction>(Channel.UNLIMITED)
    private val _effects = Channel<WorkoutOverviewEffect>(Channel.BUFFERED)
    val effects: Flow<WorkoutOverviewEffect> = _effects.receiveAsFlow()
    private val localSession = MutableStateFlow<WorkoutSession?>(null)

    val uiState: StateFlow<WorkoutOverviewUiState?> =
        combine(
            repositorySnapshots(),
            localSession,
        ) { snapshot, local ->
            snapshot?.let {
                val session = (local ?: it.sessionFromDraftOrPlan())
                    .withPreviousFromHistory(it.completed)
                WorkoutOverviewUiState(
                    session = session,
                    previousDate = previousPlanDateLabel(
                        it.completed.latestCompletedFor(planId)?.startedAt,
                    ),
                    history = it.completed,
                    canDiscardDraft = it.draft != null,
                )
            }
        }.stateInWhileInForeground(coroutineScope, null)

    init {
        coroutineScope.launch { collectActions() }
    }

    fun onAction(action: WorkoutOverviewAction) {
        when (action) {
            WorkoutOverviewAction.OnSaveDraft -> coroutineScope.launch {
                val state = requireOverviewState()
                repository.updateWorkout(
                    state.session.withPreviousFromHistory(state.history).asOverviewDraft(),
                )
            }

            WorkoutOverviewAction.OnDiscardDraft -> coroutineScope.launch {
                val draft = repository.workoutSessions(
                    planId,
                    WorkoutState.Kind.Draft,
                ).first().first()
                repository.deleteWorkout(draft.id)
                _effects.trySend(WorkoutOverviewEffect.NavigateBack)
            }

            WorkoutOverviewAction.OnStartWorkout -> coroutineScope.launch {
                val state = requireOverviewState()
                val mode = if (
                    state.canDiscardDraft || editedFromPlan(state.session, state.history)
                ) {
                    repository.updateWorkout(state.session.asOverviewDraft())
                    StartMode.StartDraft
                } else {
                    StartMode.Create
                }
                _effects.trySend(WorkoutOverviewEffect.NavigateToSession(mode))
            }

            WorkoutOverviewAction.OnAddExercise -> {
                val session = requireOverviewState().session
                _effects.trySend(
                    WorkoutOverviewEffect.OpenExerciseLibrary(session.exercises.map { it.id }),
                )
            }

            is WorkoutOverviewAction.OnOpenExerciseDetails ->
                _effects.trySend(WorkoutOverviewEffect.OpenExerciseDetails(action.exerciseId))

            is WorkoutOverviewAction.OnExercisePicked -> coroutineScope.launch {
                val exercise = repository.requireExercise(action.exerciseId)
                uiState.filterNotNull().first()
                val previous = repository
                    .workoutSessions(state = WorkoutState.Kind.Completed)
                    .first()
                    .latestCompletedExercise(action.exerciseId)
                emitAction(WorkoutOverviewAction.OnExerciseAdded(exercise, previous))
            }

            is WorkoutOverviewAction.OnRemoveExercise -> {
                requireOverviewState().session.removeExercise(action.exercise.id)
                emitAction(action)
            }

            else -> emitAction(action)
        }
    }

    private fun requireOverviewState(): WorkoutOverviewUiState =
        requireNotNull(uiState.value) { "No overview session" }

    private suspend fun editedFromPlan(
        session: WorkoutSession,
        history: List<WorkoutSession>,
    ): Boolean {
        val plan = repository.workoutPlans().first().first { it.id == planId }
        val fresh = plan.toOverviewSession(previousHistory = history)
        return session.exercises != fresh.exercises || session.note != fresh.note
    }

    private fun emitAction(action: WorkoutOverviewAction) {
        actions.trySend(action)
    }

    private fun repositorySnapshots(): Flow<OverviewRepositorySnapshot?> = combine(
        repository.workoutPlans(),
        repository.workoutSessions(state = WorkoutState.Kind.Completed),
        repository.workoutSessions(planId, WorkoutState.Kind.Draft),
    ) { plans, completed, drafts ->
        val plan = plans.firstOrNull { it.id == planId } ?: return@combine null
        OverviewRepositorySnapshot(plan, completed, drafts.firstOrNull())
    }

    private suspend fun collectActions() {
        actions.receiveAsFlow().collect { action ->
            val state = uiState.value ?: return@collect
            localSession.value = reduce(state.session, action, state.history)
        }
    }

    private fun reduce(
        session: WorkoutSession,
        action: WorkoutOverviewAction,
        history: List<WorkoutSession>,
    ): WorkoutSession {
        val reduced = reduceProgression(session, action)
            ?: reduceReorder(session, action)
            ?: reduceExerciseList(session, action)
            ?: reduceEdits(session, action)
        return if (action.rematchPrevious()) {
            reduced.withPreviousFromHistory(history)
        } else {
            reduced
        }
    }

    private fun reduceExerciseList(
        session: WorkoutSession,
        action: WorkoutOverviewAction,
    ): WorkoutSession? = when (action) {
        is WorkoutOverviewAction.OnExerciseAdded ->
            session.addExercise(action.exercise, action.previous)

        is WorkoutOverviewAction.OnRemoveExercise -> session.removeExercise(action.exercise.id)

        else -> null
    }

    private fun reduceProgression(
        session: WorkoutSession,
        action: WorkoutOverviewAction,
    ): WorkoutSession? = when (action) {
        WorkoutOverviewAction.OnIncreaseWorkout -> session.adjustAll(increase = true)

        WorkoutOverviewAction.OnDecreaseWorkout -> session.adjustAll(increase = false)

        is WorkoutOverviewAction.OnIncreaseExercise ->
            session.adjustExercise(action.exercise, increase = true)

        is WorkoutOverviewAction.OnDecreaseExercise ->
            session.adjustExercise(action.exercise, increase = false)

        is WorkoutOverviewAction.OnResetExercise ->
            session.resetExerciseToPrevious(action.exercise)

        is WorkoutOverviewAction.OnResetSet ->
            session.resetSetToPrevious(action.exercise, action.index, action.isWarmup)

        else -> null
    }

    private fun reduceReorder(
        session: WorkoutSession,
        action: WorkoutOverviewAction,
    ): WorkoutSession? = when (action) {
        is WorkoutOverviewAction.OnMoveExerciseUp ->
            session.moveExerciseUp(action.exercise.id)

        is WorkoutOverviewAction.OnMoveExerciseDown ->
            session.moveExerciseDown(action.exercise.id)

        else -> null
    }

    private fun reduceEdits(
        session: WorkoutSession,
        action: WorkoutOverviewAction,
    ): WorkoutSession = when (action) {
        is WorkoutOverviewAction.OnWeightUpdated ->
            session.updateWeightFromField(
                action.exercise,
                action.index,
                action.newWeight,
                action.isWarmup,
            )

        is WorkoutOverviewAction.OnRepsUpdated ->
            action.newReps.toIntOrNull()?.takeIf { it > 0 }?.let { reps ->
                session.updateReps(action.exercise, action.index, reps, action.isWarmup)
            } ?: session

        is WorkoutOverviewAction.OnAddSet -> session.addSet(action.exercise)

        is WorkoutOverviewAction.RemoveLastSet -> session.removeLastSet(action.exercise)

        is WorkoutOverviewAction.RemoveLastWarmupSet ->
            session.removeLastWarmupSet(action.exercise)

        is WorkoutOverviewAction.OnConvertToWarmup ->
            session.convertFirstWorkSetToWarmup(action.exercise)

        is WorkoutOverviewAction.OnConvertToWorkSet ->
            session.convertLastWarmupToWorkSet(action.exercise)

        else -> session
    }
}

private data class OverviewRepositorySnapshot(
    val plan: WorkoutPlan,
    val completed: List<WorkoutSession>,
    val draft: WorkoutSession?,
) {
    fun sessionFromDraftOrPlan(): WorkoutSession =
        draft ?: plan.toOverviewSession(previousHistory = completed)
}

private fun WorkoutOverviewAction.rematchPrevious(): Boolean = when (this) {
    is WorkoutOverviewAction.OnAddSet,
    is WorkoutOverviewAction.RemoveLastSet,
    is WorkoutOverviewAction.RemoveLastWarmupSet,
    is WorkoutOverviewAction.OnConvertToWarmup,
    is WorkoutOverviewAction.OnConvertToWorkSet,
    -> true

    else -> false
}
