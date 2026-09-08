package dev.sanastasov.bybon.workout.ui.plans

import dev.sanastasov.bybon.ui.stateInWhileInForeground
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class WorkoutPlansViewModel(
    private val repository: WorkoutsRepository,
    private val coroutineScope: CoroutineScope,
) {

    private val _effects = Channel<WorkoutPlanEffect>(Channel.BUFFERED)
    val effects: Flow<WorkoutPlanEffect> = _effects.receiveAsFlow()

    val uiState = repository.workoutPlans()
        .stateInWhileInForeground(coroutineScope, emptyList())

    fun onAction(action: WorkoutPlansAction) {
        when (action) {
            is WorkoutPlansAction.OnStartPlan -> _effects.trySend(WorkoutPlanEffect.StartPlan(action.plan))
        }
    }
}