package dev.sanastasov.bybon.workout.domain

fun WorkoutSession.updateWeightFromField(
    exercise: WorkoutExercise,
    setIndex: Int,
    text: String,
    isWarmup: Boolean = false,
): WorkoutSession {
    val trimmed = text.trim()
    val kilograms = trimmed.toFloatOrNull()
    return when {
        trimmed.isEmpty() -> updateWeight(exercise, setIndex, null, isWarmup)
        kilograms == null || kilograms < 0f -> this
        else -> updateWeight(exercise, setIndex, Weight.kilogramsOrNull(kilograms), isWarmup)
    }
}
