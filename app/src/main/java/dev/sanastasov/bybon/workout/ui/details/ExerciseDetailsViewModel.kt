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
                val exercise = checkNotNull(exercises.firstOrNull { it.id == exerciseId }) {
                    "Exercise $exerciseId is not in the repository"
                }
                exercise.toDetailsUi()
            }
            .stateInWhileInForeground(coroutineScope, ExerciseDetailsUiState.Loading)
}
