package dev.sanastasov.bybon.workout.ui.history

import dev.sanastasov.bybon.ui.stateInWhileInForeground
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class WorkoutHistoryViewModel(
    repository: WorkoutsRepository,
    coroutineScope: CoroutineScope,
) {

    val uiState: StateFlow<List<WorkoutSessionHistoryUi>> =
        repository.workoutSessions()
            .map { sessions -> sessions.toHistoryUi() }
            .stateInWhileInForeground(coroutineScope, emptyList())
}
