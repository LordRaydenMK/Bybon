package dev.sanastasov.bybon.workout.domain

import kotlin.time.Duration

data class ExerciseDefinition(
    val id: String,
    val name: String,
    val primaryMuscleGroup: MuscleGroup,
    val equipment: Equipment,
    val mechanic: Mechanic,
)

val ExerciseDefinition.defaultRest: Duration
    get() = mechanic.defaultRest

fun Duration.formatRestClock(): String {
    val totalSeconds = inWholeSeconds.coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
