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
    val toIndex = fromIndex + direction.offset
    check(toIndex in movable.indices) {
        "Cannot move ${planId.id} ${direction.name.lowercase()}"
    }
    val reordered = movable.toMutableList().apply {
        add(toIndex, removeAt(fromIndex))
    }.iterator()
    return map { current ->
        if (canMove(current)) reordered.next() else current
    }
}
