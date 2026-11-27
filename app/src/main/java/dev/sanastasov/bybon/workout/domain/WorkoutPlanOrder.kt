package dev.sanastasov.bybon.workout.domain

fun List<WorkoutPlan>.movePlan(
    planId: WorkoutPlanId,
    direction: ReorderDirection,
    canMove: (WorkoutPlan) -> Boolean,
): List<WorkoutPlan> {
    val plan = firstOrNull { it.id == planId } ?: error("Plan ${planId.id} does not exist")
    check(canMove(plan)) { "Cannot move ${planId.id}" }
    val movable = filter(canMove)
    val fromIndex = movable.indexOfFirst { it.id == planId }
    val neighborIndex = fromIndex + direction.offset
    check(neighborIndex in movable.indices) {
        "Cannot move ${planId.id} ${direction.name.lowercase()}"
    }
    val neighbor = movable[neighborIndex]
    return map { current ->
        when (current.id) {
            plan.id -> neighbor
            neighbor.id -> plan
            else -> current
        }
    }
}
