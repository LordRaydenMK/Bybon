package dev.sanastasov.bybon.workout.ui.session

import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutPlansRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WorkoutSessionViewModel(
    val planId: WorkoutPlanId,
    val repository: WorkoutPlansRepository,
    val coroutineScope: CoroutineScope,
) {

    private val sessionFlow = repository.workoutPlans()
        .map { plans -> plans.first { it.id == planId }.toWorkoutSessionUi() }

    val uiState: StateFlow<WorkoutSessionUiState?>
        field = MutableStateFlow(null)

    init {
        coroutineScope.launch {
            uiState.value = sessionFlow.first()
        }
    }

    fun onAction(action: WorkoutSessionAction) {
        uiState.update { it?.completeExercise() }
    }
}