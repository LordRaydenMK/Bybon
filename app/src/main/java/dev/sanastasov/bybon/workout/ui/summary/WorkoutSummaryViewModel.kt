package dev.sanastasov.bybon.workout.ui.summary

import dev.sanastasov.bybon.ui.stateInWhileInForeground
import dev.sanastasov.bybon.workout.domain.WorkoutSessionId
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class WorkoutSummaryViewModel(
    private val sessionId: WorkoutSessionId,
    repository: WorkoutsRepository,
    coroutineScope: CoroutineScope,
) {

    val uiState: StateFlow<WorkoutSummaryUiState> =
        repository.workoutSessions()
            .map { sessions -> sessions.requireCompletedSummary(sessionId) }
            .stateInWhileInForeground(coroutineScope, WorkoutSummaryUiState.Loading)
}
