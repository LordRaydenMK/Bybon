package dev.sanastasov.bybon.workout.ui.session

import dev.sanastasov.bybon.workout.RestPromoteScheduler
import dev.sanastasov.bybon.workout.domain.ExerciseState
import dev.sanastasov.bybon.workout.domain.ReorderDirection
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutSessionId
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import dev.sanastasov.bybon.workout.domain.activeRestTimer
import dev.sanastasov.bybon.workout.domain.addExercise
import dev.sanastasov.bybon.workout.domain.addSet
import dev.sanastasov.bybon.workout.domain.adjustExercise
import dev.sanastasov.bybon.workout.domain.completeSet
import dev.sanastasov.bybon.workout.domain.convertFirstWorkSetToWarmup
import dev.sanastasov.bybon.workout.domain.convertLastWarmupToWorkSet
import dev.sanastasov.bybon.workout.domain.latestCompletedExercise
import dev.sanastasov.bybon.workout.domain.moveExercise
import dev.sanastasov.bybon.workout.domain.removeExercise
import dev.sanastasov.bybon.workout.domain.removeLastSet
import dev.sanastasov.bybon.workout.domain.requireExercise
import dev.sanastasov.bybon.workout.domain.resetExerciseToPrevious
import dev.sanastasov.bybon.workout.domain.resetSetToPrevious
import dev.sanastasov.bybon.workout.domain.startWorkout
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import dev.sanastasov.bybon.workout.domain.updateReps
import dev.sanastasov.bybon.workout.domain.updateWeightFromField
import dev.sanastasov.bybon.workout.domain.withPreviousFromHistory
import dev.sanastasov.bybon.workout.restPromoteWait
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WorkoutSessionViewModel(
    val planId: WorkoutPlanId,
    val start: StartMode,
    val repository: WorkoutsRepository,
    val coroutineScope: CoroutineScope,
    private val restPromoteScheduler: RestPromoteScheduler = RestPromoteScheduler.NoOp,
) {
    private val _effects = Channel<WorkoutSessionEffect>(Channel.BUFFERED)
    val effects: Flow<WorkoutSessionEffect> = _effects.receiveAsFlow()
    private val active = ActiveSession(planId, start, repository)
    val uiState: StateFlow<WorkoutSession?> = active.uiState

    init {
        coroutineScope.launch { active.loadOrCreate() }
        coroutineScope.launch { active.persistWrites() }
    }

    fun onAction(action: WorkoutSessionAction) {
        coroutineScope.launch {
            val pending = active.mutations.withLock {
                if (active.closed) {
                    null
                } else {
                    apply(action)
                }
            }
            when (pending) {
                is Pending.Delete -> {
                    active.delete(pending.id)
                    _effects.trySend(WorkoutSessionEffect.StopService)
                    _effects.trySend(WorkoutSessionEffect.NavigateBack)
                }

                is Pending.Summary -> {
                    active.awaitPersisted()
                    if (pending.stopService) {
                        _effects.trySend(WorkoutSessionEffect.StopService)
                    }
                    _effects.trySend(WorkoutSessionEffect.NavigateToSummary(pending.id))
                }

                null -> Unit
            }
        }
    }

    private suspend fun apply(action: WorkoutSessionAction): Pending? = when (action) {
        WorkoutSessionAction.OnCancelWorkout -> {
            val session = active.uiState.value ?: return null
            restPromoteScheduler.cancel()
            // The screen collects effects only while uiState is set.
            active.close()
            Pending.Delete(session.id)
        }

        WorkoutSessionAction.OnAddExercise -> {
            val session = checkNotNull(active.uiState.value) { "No active session" }
            _effects.trySend(
                WorkoutSessionEffect.OpenExerciseLibrary(session.exercises.map { it.id }),
            )
            null
        }

        is WorkoutSessionAction.OnExercisePicked -> {
            addPickedExercise(action.exerciseId)
            null
        }

        is WorkoutSessionAction.OnRemoveExercise ->
            removeSessionExercise(action)?.let { sessionId ->
                Pending.Summary(sessionId, stopService = false)
            }

        is WorkoutSessionAction.OnOpenExerciseDetails -> {
            _effects.trySend(WorkoutSessionEffect.OpenExerciseDetails(action.exerciseId))
            null
        }

        else -> {
            val session = checkNotNull(active.uiState.value) { "No active session" }
            val updated = reduce(session, action, active.history)
            active.publish(updated)
            emitAfterMove(session, updated, action)
            if (emitAfterSetCompleted(updated, action)) {
                Pending.Summary(updated.id, stopService = true)
            } else {
                null
            }
        }
    }

    private suspend fun addPickedExercise(exerciseId: String) {
        val exercise = repository.requireExercise(exerciseId)
        val previous = repository.workoutSessions().first()
            .latestCompletedExercise(exerciseId)
        val session = checkNotNull(active.uiState.value) { "No active session" }
        active.publish(session.addExercise(exercise, previous))
    }

    private suspend fun removeSessionExercise(
        action: WorkoutSessionAction.OnRemoveExercise,
    ): WorkoutSessionId? {
        val session = checkNotNull(active.uiState.value) { "No active session" }
        val removedIndex = session.exercises.indexOfFirst { it.id == action.exercise.id }
        val updated = session.removeExercise(action.exercise.id)
        active.publish(updated)
        return if (updated.state is WorkoutState.Completed) {
            updated.id
        } else {
            if (removedIndex >= 0) {
                _effects.trySend(
                    WorkoutSessionEffect.ScrollToExercise((removedIndex - 1).coerceAtLeast(0)),
                )
            }
            null
        }
    }

    private fun emitAfterSetCompleted(
        updated: WorkoutSession,
        action: WorkoutSessionAction,
    ): Boolean = if (action is WorkoutSessionAction.OnCompleteSet) {
        syncRestPromoteAlarm(updated)
        if (updated.state is WorkoutState.Completed) {
            true
        } else {
            val completedIndex = updated.exercises.indexOfFirst { it.id == action.exercise.id }
            val nextPage = completedIndex + 1
            val completed = updated.exercises.getOrNull(completedIndex)
            if (completed?.state == ExerciseState.Completed &&
                nextPage < updated.exercises.size
            ) {
                _effects.trySend(WorkoutSessionEffect.ScrollToExercise(nextPage))
            }
            false
        }
    } else {
        false
    }

    private fun emitAfterMove(
        before: WorkoutSession,
        updated: WorkoutSession,
        action: WorkoutSessionAction,
    ) {
        val exerciseId = when (action) {
            is WorkoutSessionAction.OnMoveExerciseUp -> action.exercise.id
            is WorkoutSessionAction.OnMoveExerciseDown -> action.exercise.id
            else -> return
        }
        val movedTo = updated.exercises.indexOfFirst { it.id == exerciseId }
        if (movedTo < 0) return
        val currentId = before.currentExerciseIndex?.let { before.exercises[it].id }
        val page = if (action is WorkoutSessionAction.OnMoveExerciseDown &&
            currentId == exerciseId
        ) {
            requireNotNull(updated.currentExerciseIndex) { "No current exercise" }
        } else {
            movedTo
        }
        _effects.trySend(WorkoutSessionEffect.ScrollToExercise(page))
    }

    private fun syncRestPromoteAlarm(
        session: WorkoutSession,
        now: LocalDateTime = LocalDateTime.now(),
    ) {
        val rest = session.activeRestTimer(now)
        if (rest == null) {
            restPromoteScheduler.cancel()
            return
        }
        val wait = restPromoteWait(rest.remaining(now))
        if (wait > Duration.ZERO) {
            restPromoteScheduler.schedule(wait)
        } else {
            restPromoteScheduler.cancel()
        }
    }

    private fun reduce(
        session: WorkoutSession,
        action: WorkoutSessionAction,
        history: List<WorkoutSession>,
    ): WorkoutSession {
        val reduced = reduceEdits(session, action) ?: reduceProgression(session, action)
        return if (action.rematchPrevious()) {
            reduced.withPreviousFromHistory(history)
        } else {
            reduced
        }
    }

    private fun reduceEdits(
        session: WorkoutSession,
        action: WorkoutSessionAction,
    ): WorkoutSession? = when (action) {
        is WorkoutSessionAction.OnWeightUpdated ->
            session.updateWeightFromField(
                action.exercise,
                action.index,
                action.newWeight,
                action.isWarmup,
            )

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

        is WorkoutSessionAction.OnMoveExerciseUp ->
            session.moveExercise(action.exercise.id, ReorderDirection.Up)

        is WorkoutSessionAction.OnMoveExerciseDown ->
            session.moveExercise(action.exercise.id, ReorderDirection.Down)

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

    private sealed interface Pending {
        data class Delete(
            val id: WorkoutSessionId,
        ) : Pending

        data class Summary(
            val id: WorkoutSessionId,
            val stopService: Boolean,
        ) : Pending
    }
}

private class ActiveSession(
    private val planId: WorkoutPlanId,
    private val start: StartMode,
    private val repository: WorkoutsRepository,
) {
    val mutations = Mutex()
    private val database = Mutex()
    private val writes = Channel<Unit>(Channel.CONFLATED)
    private val savedGeneration = MutableStateFlow(0)
    private val state = MutableStateFlow<WorkoutSession?>(null)
    val uiState: StateFlow<WorkoutSession?> = state

    var history: List<WorkoutSession> = emptyList()
        private set

    private var generation = 0

    @Volatile
    var closed: Boolean = false
        private set

    fun publish(updated: WorkoutSession) {
        state.value = updated
        generation += 1
        writes.trySend(Unit)
    }

    fun close() {
        closed = true
    }

    suspend fun loadOrCreate() {
        mutations.withLock {
            val sessions = repository.workoutSessions().first()
            history = sessions.filter { it.state is WorkoutState.Completed }
            state.value = openSession(sessions)
        }
    }

    private suspend fun openSession(sessions: List<WorkoutSession>): WorkoutSession = when (start) {
        StartMode.Create -> createSession(sessions)
        StartMode.Resume -> resumeSession(sessions)
        StartMode.StartDraft -> startDraft(sessions)
    }

    private suspend fun createSession(sessions: List<WorkoutSession>): WorkoutSession {
        val existing = sessions.firstOrNull { session ->
            session.planId == planId && session.state !is WorkoutState.Completed
        }
        if (existing != null) return existing
        val plan = repository.workoutPlans().first().first { it.id == planId }
        val now = LocalDateTime.now()
        return plan.toWorkoutSession(
            previousHistory = sessions,
            startedAt = now,
            bodyWeight = repository.sevenDayBodyWeight(now.toLocalDate()),
        ).also { created -> repository.updateWorkout(created) }
    }

    private suspend fun resumeSession(sessions: List<WorkoutSession>): WorkoutSession {
        val existing = sessions.first { it.planId == planId && it.isResumable }
        if (existing.workoutSets.any { it.setState == SetState.InProgress }) return existing
        return existing.startWorkout().also { started -> repository.updateWorkout(started) }
    }

    private suspend fun startDraft(sessions: List<WorkoutSession>): WorkoutSession {
        val draft = sessions.firstOrNull { it.planId == planId && it.isDraft }
        val session = draft ?: sessions.first { it.planId == planId && it.isResumable }
        if (session.workoutSets.any { it.setState == SetState.InProgress }) return session
        val now = LocalDateTime.now()
        return session.startWorkout(
            now = now,
            snapshot = repository.sevenDayBodyWeight(now.toLocalDate()),
        ).also { started -> repository.updateWorkout(started) }
    }

    suspend fun awaitPersisted() {
        val target = mutations.withLock { generation }
        if (savedGeneration.value >= target) return
        writes.trySend(Unit)
        savedGeneration.first { saved -> saved >= target || closed }
    }

    suspend fun delete(id: WorkoutSessionId) {
        database.withLock { repository.deleteWorkout(id) }
    }

    suspend fun persistWrites() {
        while (true) {
            writes.receive()
            val captured = mutations.withLock {
                val current = state.value
                if (closed || current == null || generation == savedGeneration.value) {
                    null
                } else {
                    generation to current
                }
            } ?: continue
            database.withLock {
                if (!closed) repository.updateWorkout(captured.second)
            }
            mutations.withLock {
                if (!closed && generation == captured.first) {
                    savedGeneration.value = captured.first
                }
            }
        }
    }
}

private fun WorkoutSessionAction.rematchPrevious(): Boolean = when (this) {
    is WorkoutSessionAction.OnAddSet,
    is WorkoutSessionAction.RemoveLastSet,
    is WorkoutSessionAction.OnConvertToWarmup,
    is WorkoutSessionAction.OnConvertToWorkSet,
    -> true

    else -> false
}
