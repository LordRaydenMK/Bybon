package dev.sanastasov.bybon.workout.ui.summary

import dev.sanastasov.bybon.ui.stateInWhileInForeground
import dev.sanastasov.bybon.workout.domain.WorkoutSessionId
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class WorkoutSummaryViewModel(
    private val sessionId: WorkoutSessionId,
    private val repository: WorkoutsRepository,
    private val coroutineScope: CoroutineScope,
    now: () -> LocalDateTime = { LocalDateTime.now() },
) {

    private val _effects = Channel<WorkoutSummaryEffect>(Channel.BUFFERED)
    val effects: Flow<WorkoutSummaryEffect> = _effects.receiveAsFlow()

    val uiState: StateFlow<WorkoutSummaryUiState> =
        repository.workoutSessions()
            .mapNotNull { sessions -> sessions.completedSummary(sessionId, now()) }
            .stateInWhileInForeground(coroutineScope, WorkoutSummaryUiState.Loading)

    fun onAction(action: WorkoutSummaryAction) {
        when (action) {
            is WorkoutSummaryAction.OnOpenExerciseDetails ->
                _effects.trySend(WorkoutSummaryEffect.OpenExerciseDetails(action.exerciseId))

            WorkoutSummaryAction.OnDeleteSession -> coroutineScope.launch {
                repository.deleteWorkout(sessionId)
                _effects.trySend(WorkoutSummaryEffect.NavigateBack)
            }
        }
    }
}
