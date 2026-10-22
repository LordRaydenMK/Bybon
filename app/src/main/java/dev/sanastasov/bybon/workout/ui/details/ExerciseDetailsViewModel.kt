package dev.sanastasov.bybon.workout.ui.details

import dev.sanastasov.bybon.ui.stateInWhileInForeground
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class ExerciseDetailsViewModel(
    private val exerciseId: String,
    repository: WorkoutsRepository,
    coroutineScope: CoroutineScope,
) {

    val uiState: StateFlow<ExerciseDetailsUiState> =
        repository.exercises()
            .map { exercises ->
                exercises.firstOrNull { it.id == exerciseId }?.toDetailsUi()
                    ?: ExerciseDetailsUiState.Missing
            }
            .stateInWhileInForeground(coroutineScope, ExerciseDetailsUiState.Loading)
}
