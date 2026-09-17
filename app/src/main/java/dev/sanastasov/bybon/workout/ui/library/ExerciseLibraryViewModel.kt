package dev.sanastasov.bybon.workout.ui.library

import dev.sanastasov.bybon.ui.stateInWhileInForeground
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import dev.sanastasov.bybon.workout.domain.addExercise
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
    private val planId: WorkoutPlanId,
    private val repository: WorkoutsRepository,
    private val coroutineScope: CoroutineScope,
) {

    private val _effects = Channel<ExerciseLibraryEffect>(Channel.BUFFERED)
    val effects: Flow<ExerciseLibraryEffect> = _effects.receiveAsFlow()

    private val selectedExerciseId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ExerciseLibraryUiState> = combine(
        repository.exercises(),
        selectedExerciseId,
    ) { exercises, selectedId ->
        ExerciseLibraryUiState(exercises.groupedByBodyPart(selectedId))
    }.stateInWhileInForeground(coroutineScope, ExerciseLibraryUiState())

    fun onAction(action: ExerciseLibraryAction) {
        when (action) {
            is ExerciseLibraryAction.OnToggleExercise ->
                selectedExerciseId.update { current ->
                    if (current == action.exerciseId) null else action.exerciseId
                }

            ExerciseLibraryAction.OnAddExercise -> coroutineScope.launch {
                val exerciseId = selectedExerciseId.value ?: return@launch
                val exercise = repository.exercises().first()
                    .firstOrNull { it.id == exerciseId }
                    ?: return@launch
                repository.updatePlan(planId) { it.addExercise(exercise) }
                _effects.trySend(ExerciseLibraryEffect.NavigateBack)
            }
        }
    }
}
