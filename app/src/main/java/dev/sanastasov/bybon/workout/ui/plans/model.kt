package dev.sanastasov.bybon.workout.ui.plans

import dev.sanastasov.bybon.workout.domain.Equipment
import dev.sanastasov.bybon.workout.domain.PlanedExercise
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.formatRestClock
import kotlin.time.Duration

data class WorkoutPlansUiState(
    val plans: List<WorkoutPlanUi> = emptyList(),
    val showArchived: Boolean = false,
) {
    val unarchivedPlans: List<WorkoutPlanUi> get() = plans.filter { !it.plan.isArchived }

    val archivedPlans: List<WorkoutPlanUi> get() = plans.filter { it.plan.isArchived }

    val hasArchivedPlans: Boolean get() = archivedPlans.isNotEmpty()

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
    data class OnAddSet(
        val exerciseId: String,
    ) : EditPlanAction()
    data class OnRemoveLastSet(
        val exerciseId: String,
    ) : EditPlanAction()
    data class OnRemoveExercise(
        val exerciseId: String,
    ) : EditPlanAction()
}

sealed class EditPlanEffect {
    data object NavigateBack : EditPlanEffect()
}

data class PlannedSetUi(
    val isWarmup: Boolean,
    val workSetNumber: Int?,
    val repsLabel: String,
    val rest: Duration?,
)

fun PlanedExercise.subtitle(): String =
    "${exercise.primaryMuscleGroup.name} · ${exercise.equipment.label}"

fun PlanedExercise.toPlannedSets(): List<PlannedSetUi> {
    val warmup = List(warmupSets) {
        PlannedSetUi(
            isWarmup = true,
            workSetNumber = null,
            repsLabel = "—",
            rest = null,
        )
    }
    val work = (1..sets).map { number ->
        PlannedSetUi(
            isWarmup = false,
            workSetNumber = number,
            repsLabel = "${repRange.first}–${repRange.last}",
            rest = restAfterWorkSet,
        )
    }
    return warmup + work
}

fun PlannedSetUi.contentDescription(exerciseName: String): String = if (isWarmup) {
    "$exerciseName warmup set"
} else {
    val restLabel = rest?.let { ", rest ${it.formatRestClock()}" }.orEmpty()
    "$exerciseName set $workSetNumber, $repsLabel reps$restLabel"
}

private val Equipment.label: String
    get() = when (this) {
        Equipment.AssistedBodyWeight -> "Assisted"
        else -> name
    }
