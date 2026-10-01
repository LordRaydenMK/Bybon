package dev.sanastasov.bybon.workout.domain

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class ExerciseDefinition(
    val id: String,
    val name: String,
    val primaryMuscleGroup: MuscleGroup,
    val equipment: Equipment,
    // Null only for a Strong import whose name has no mapped mechanic. Strong does not record one.
    val mechanic: Mechanic? = null,
)

val ExerciseDefinition.defaultRest: Duration
    get() = mechanic?.defaultRest ?: 90.seconds

fun Duration.formatRestClock(): String {
    val totalSeconds = inWholeSeconds.coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
