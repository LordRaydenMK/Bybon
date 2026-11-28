package dev.sanastasov.bybon.workout.domain

fun WorkoutPlan.rename(name: String): WorkoutPlan {
    val trimmed = name.trim()
    require(trimmed.isNotEmpty()) { "Plan name must not be blank" }
    return copy(name = trimmed)
}

fun WorkoutPlan.duplicate(newId: WorkoutPlanId): WorkoutPlan {
    require(newId != id) { "Duplicated plan needs a new id" }
    return copy(id = newId, name = "Copy of $name")
}

fun WorkoutPlan.withNote(note: String): WorkoutPlan =
    copy(description = note.trim().ifEmpty { null })
