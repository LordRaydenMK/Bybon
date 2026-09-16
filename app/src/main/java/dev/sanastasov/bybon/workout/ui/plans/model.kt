package dev.sanastasov.bybon.workout.ui.plans

import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.WorkoutSession

data class WorkoutPlansUiState(
    val plans: List<WorkoutPlanUi> = emptyList(),
    val showArchived: Boolean = false,
    val hasArchivedPlans: Boolean = false,
) {
    val activePlans: List<WorkoutPlanUi> get() = plans.filter { !it.plan.isArchived }

    val archivedPlans: List<WorkoutPlanUi> get() = plans.filter { it.plan.isArchived }

    val showMyPlansHeading: Boolean get() = showArchived

    val showArchivedPlansHeading: Boolean get() = showArchived && archivedPlans.isNotEmpty()

    val showArchivedPlansButton: Boolean get() = !showArchived && hasArchivedPlans

    val hideArchivedPlansButton: Boolean get() = showArchived
}

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
