package dev.sanastasov.bybon.workout.ui.plans

import dev.sanastasov.bybon.workout.domain.WorkoutPlan

sealed class WorkoutPlansAction {
    data class OnStartPlan(val plan: WorkoutPlan) : WorkoutPlansAction()
}

sealed class WorkoutPlanEffect {
    data class StartPlan(val plan: WorkoutPlan) : WorkoutPlanEffect()
}
