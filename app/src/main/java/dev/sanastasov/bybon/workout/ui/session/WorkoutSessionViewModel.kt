package dev.sanastasov.bybon.workout.ui.session

import dev.sanastasov.bybon.ui.stateInWhileInForeground
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutPlansRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map

class WorkoutSessionViewModel(
    val planId: WorkoutPlanId,
    val repository: WorkoutPlansRepository,
    val coroutineScope: CoroutineScope,
) {

    val uiState = repository.workoutPlans()
        .map { plans -> plans.first { it.id == planId } }
        .map { plan ->
            WorkoutSessionUiState(plan, 0)
        }
        .stateInWhileInForeground(coroutineScope, null)
}