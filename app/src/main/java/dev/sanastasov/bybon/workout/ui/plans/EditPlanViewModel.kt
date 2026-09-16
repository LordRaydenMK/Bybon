package dev.sanastasov.bybon.workout.ui.plans

import dev.sanastasov.bybon.ui.stateInWhileInForeground
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map

class EditPlanViewModel(
    private val planId: WorkoutPlanId,
    repository: WorkoutsRepository,
    coroutineScope: CoroutineScope,
) {

    val uiState = repository.workoutPlans()
        .map { plans -> plans.firstOrNull { it.id == planId } }
        .stateInWhileInForeground(coroutineScope, null)
}
