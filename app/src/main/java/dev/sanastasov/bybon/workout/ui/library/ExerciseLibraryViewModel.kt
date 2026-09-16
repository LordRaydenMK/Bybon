package dev.sanastasov.bybon.workout.ui.library

import dev.sanastasov.bybon.ui.stateInWhileInForeground
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class ExerciseLibraryViewModel(
    repository: WorkoutsRepository,
    coroutineScope: CoroutineScope,
) {

    val uiState: StateFlow<ExerciseLibraryUiState> = repository.exercises()
        .map { ExerciseLibraryUiState(it.groupedByBodyPart()) }
        .stateInWhileInForeground(coroutineScope, ExerciseLibraryUiState())
}
