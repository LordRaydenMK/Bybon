package dev.sanastasov.bybon.workout.ui.summary

import dev.sanastasov.bybon.ui.stateInWhileInForeground
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import dev.sanastasov.bybon.workout.domain.completedSessionKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class WorkoutSummaryViewModel(
    private val sessionKey: String,
    repository: WorkoutsRepository,
    coroutineScope: CoroutineScope,
) {

    val uiState: StateFlow<WorkoutSummaryUiState> =
        repository.workoutSessions()
            .map { sessions ->
                sessions.firstOrNull { it.completedSessionKey() == sessionKey }
                    ?.toSummaryUi()
                    ?: WorkoutSummaryUiState.NotFound
            }
            .stateInWhileInForeground(coroutineScope, WorkoutSummaryUiState.Loading)
}
