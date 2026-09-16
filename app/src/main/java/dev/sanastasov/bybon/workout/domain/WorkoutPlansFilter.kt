package dev.sanastasov.bybon.workout.domain

enum class WorkoutPlansFilter {
    ActivePlans,
    AllPlans,
}

fun List<WorkoutPlan>.filterBy(plansFilter: WorkoutPlansFilter): List<WorkoutPlan> =
    when (plansFilter) {
        WorkoutPlansFilter.ActivePlans -> filter { !it.isArchived }
        WorkoutPlansFilter.AllPlans -> this
    }
