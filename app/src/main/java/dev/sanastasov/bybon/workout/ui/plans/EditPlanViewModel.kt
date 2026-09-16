package dev.sanastasov.bybon.workout.ui.plans

import dev.sanastasov.bybon.ui.stateInWhileInForeground
import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import dev.sanastasov.bybon.workout.domain.addWorkSet
import dev.sanastasov.bybon.workout.domain.removeExercise
import dev.sanastasov.bybon.workout.domain.removeLastWorkSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class EditPlanViewModel(
    private val planId: WorkoutPlanId,
    private val repository: WorkoutsRepository,
    private val coroutineScope: CoroutineScope,
) {

    private val _effects = Channel<EditPlanEffect>(Channel.BUFFERED)
    val effects: Flow<EditPlanEffect> = _effects.receiveAsFlow()

    val uiState: StateFlow<WorkoutPlan?> = repository.workoutPlans()
        .map { plans -> plans.firstOrNull { it.id == planId } }
        .stateInWhileInForeground(coroutineScope, null)

    fun onAction(action: EditPlanAction) {
        coroutineScope.launch {
            when (action) {
                EditPlanAction.OnArchivePlan -> {
                    repository.archivePlan(planId, archived = true)
                    _effects.trySend(EditPlanEffect.NavigateBack)
                }

                is EditPlanAction.OnAddSet ->
                    repository.updatePlan(planId) { it.addWorkSet(action.exerciseId) }

                is EditPlanAction.OnRemoveLastSet ->
                    repository.updatePlan(planId) { it.removeLastWorkSet(action.exerciseId) }

                is EditPlanAction.OnRemoveExercise ->
                    repository.updatePlan(planId) { it.removeExercise(action.exerciseId) }
            }
        }
    }
}
