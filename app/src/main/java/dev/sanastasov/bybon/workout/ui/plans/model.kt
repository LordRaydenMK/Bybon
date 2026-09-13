package dev.sanastasov.bybon.workout.ui.plans

import dev.sanastasov.bybon.workout.domain.WorkoutPlan

data class WorkoutPlanUi(
    val plan: WorkoutPlan,
    val isActive: Boolean,
)

sealed class WorkoutPlansAction {
    data class OnStartPlan(val plan: WorkoutPlan, val isResume: Boolean) : WorkoutPlansAction()
}

sealed class WorkoutPlanEffect {
    data class StartPlan(val plan: WorkoutPlan, val isResume: Boolean) : WorkoutPlanEffect()
}
