package dev.sanastasov.bybon.workout.ui.plans

import dev.sanastasov.bybon.ui.stateInWhileInForeground
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow

class WorkoutPlansViewModel(
    private val repository: WorkoutsRepository,
    private val coroutineScope: CoroutineScope,
) {

    private val _effects = Channel<WorkoutPlanEffect>(Channel.BUFFERED)
    val effects: Flow<WorkoutPlanEffect> = _effects.receiveAsFlow()

    val uiState = combine(
        repository.workoutPlans(),
        repository.workoutSessions(),
    ) { plans, sessions ->
        plans.map { it.toUi(sessions) }
    }.stateInWhileInForeground(coroutineScope, emptyList())

    fun onAction(action: WorkoutPlansAction) {
        when (action) {
            is WorkoutPlansAction.OnStartPlan -> {
                val isResume = uiState.value.any { it.plan.id == action.plan.id && it.isActive }
                val effect = if (isResume) {
                    WorkoutPlanEffect.OpenSession(action.plan)
                } else {
                    WorkoutPlanEffect.OpenOverview(action.plan)
                }
                _effects.trySend(effect)
            }

            is WorkoutPlansAction.OnEditPlan -> {
                _effects.trySend(WorkoutPlanEffect.OpenEditPlan(action.plan))
            }
        }
    }
}
