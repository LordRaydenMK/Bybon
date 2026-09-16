package dev.sanastasov.bybon.workout.ui.plans

import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.WorkoutSession

data class WorkoutPlansUiState(
    val activePlans: List<WorkoutPlanUi> = emptyList(),
    val archivedPlans: List<WorkoutPlanUi> = emptyList(),
    val showArchived: Boolean = false,
)

data class WorkoutPlanUi(
    val plan: WorkoutPlan,
    val isActive: Boolean,
)

fun WorkoutPlan.toUi(sessions: List<WorkoutSession>): WorkoutPlanUi = WorkoutPlanUi(
    plan = this,
    isActive = !isArchived &&
        sessions.any { session ->
            session.planId == id &&
                session.workoutSets.any { it.setState == SetState.InProgress }
        },
)

sealed class WorkoutPlansAction {
    data class OnStartPlan(
        val plan: WorkoutPlan,
    ) : WorkoutPlansAction()

    data class OnEditPlan(
        val plan: WorkoutPlan,
    ) : WorkoutPlansAction()

    data class OnArchivePlan(
        val plan: WorkoutPlan,
    ) : WorkoutPlansAction()

    data class OnUnarchivePlan(
        val plan: WorkoutPlan,
    ) : WorkoutPlansAction()

    data object OnShowArchivedPlans : WorkoutPlansAction()

    data object OnHideArchivedPlans : WorkoutPlansAction()
}

sealed class WorkoutPlanEffect {
    data class OpenOverview(
        val plan: WorkoutPlan,
    ) : WorkoutPlanEffect()
    data class OpenSession(
        val plan: WorkoutPlan,
    ) : WorkoutPlanEffect()
    data class OpenEditPlan(
        val plan: WorkoutPlan,
    ) : WorkoutPlanEffect()
}

sealed class EditPlanAction {
    data object OnArchivePlan : EditPlanAction()
}

sealed class EditPlanEffect {
    data object NavigateBack : EditPlanEffect()
}
