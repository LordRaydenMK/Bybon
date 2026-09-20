package dev.sanastasov.bybon.workout.ui.library

import dev.sanastasov.bybon.ui.stateInWhileInForeground
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExerciseLibraryViewModel(
    private val existingExerciseIds: List<String>,
    private val repository: WorkoutsRepository,
    private val coroutineScope: CoroutineScope,
) {

    private val _effects = Channel<ExerciseLibraryEffect>(Channel.BUFFERED)
    val effects: Flow<ExerciseLibraryEffect> = _effects.receiveAsFlow()

    private val selectedExerciseId = MutableStateFlow<String?>(null)
    private val filters = MutableStateFlow(ExerciseLibraryFilters())

    val uiState: StateFlow<ExerciseLibraryUiState> = combine(
        repository.exercises(),
        selectedExerciseId,
        filters,
    ) { exercises, selectedId, currentFilters ->
        exercises.toLibraryUiState(
            selectedExerciseId = selectedId,
            filters = currentFilters,
            existingExerciseIds = existingExerciseIds,
        )
    }.stateInWhileInForeground(coroutineScope, ExerciseLibraryUiState())

    fun onAction(action: ExerciseLibraryAction) {
        when (action) {
            is ExerciseLibraryAction.OnToggleExercise -> {
                if (uiState.value.isExisting(action.exerciseId)) return
                selectedExerciseId.update { current ->
                    if (current == action.exerciseId) null else action.exerciseId
                }
            }

            is ExerciseLibraryAction.OnAddExercise -> coroutineScope.launch {
                val exists = repository.exercises().first()
                    .any { it.id == action.exerciseId }
                if (!exists) return@launch
                _effects.trySend(ExerciseLibraryEffect.ExercisePicked(action.exerciseId))
            }

            is ExerciseLibraryAction.OnToggleFilter ->
                filters.update { it.toggle(action.id) }
        }
    }
}
