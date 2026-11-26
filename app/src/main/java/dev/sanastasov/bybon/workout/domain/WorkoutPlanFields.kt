package dev.sanastasov.bybon.workout.domain

fun WorkoutPlan.rename(name: String): WorkoutPlan {
    val trimmed = name.trim()
    require(trimmed.isNotEmpty()) { "Plan name must not be blank" }
    return copy(name = trimmed)
}

fun WorkoutPlan.withNote(note: String): WorkoutPlan =
    copy(description = note.trim().ifEmpty { null })
