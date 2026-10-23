package dev.sanastasov.bybon.workout.ui.summary

import dev.sanastasov.bybon.ui.stateInWhileInForeground
import dev.sanastasov.bybon.workout.domain.WorkoutSessionId
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow

class WorkoutSummaryViewModel(
    private val sessionId: WorkoutSessionId,
    repository: WorkoutsRepository,
    coroutineScope: CoroutineScope,
) {

    private val _effects = Channel<WorkoutSummaryEffect>(Channel.BUFFERED)
    val effects: Flow<WorkoutSummaryEffect> = _effects.receiveAsFlow()

    val uiState: StateFlow<WorkoutSummaryUiState> =
        repository.workoutSessions()
            .map { sessions -> sessions.requireCompletedSummary(sessionId) }
            .stateInWhileInForeground(coroutineScope, WorkoutSummaryUiState.Loading)

    fun onAction(action: WorkoutSummaryAction) {
        when (action) {
            is WorkoutSummaryAction.OnOpenExerciseDetails ->
                _effects.trySend(WorkoutSummaryEffect.OpenExerciseDetails(action.exerciseId))
        }
    }
}
