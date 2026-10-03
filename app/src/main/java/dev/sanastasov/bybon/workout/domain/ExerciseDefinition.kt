package dev.sanastasov.bybon.workout.domain

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class ExerciseDefinition(
    val id: String,
    val name: String,
    // Null only for a Strong import whose name has no mapped body part. Strong does not record one.
    val bodyPart: BodyPart? = null,
    // Null only for a Strong import whose equipment cannot be read from the map or the name.
    val equipment: Equipment? = null,
    // Null only for a Strong import whose name has no mapped mechanic. Strong does not record one.
    val mechanic: Mechanic? = null,
    val primaryMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    // RepDB-style equipment slug. Null when the row lists no equipment, or a Strong import cannot name one.
    val equipmentSlug: String? = null,
)

val ExerciseDefinition.defaultRest: Duration
    get() = mechanic?.defaultRest ?: 90.seconds

fun Duration.formatRestClock(): String {
    val totalSeconds = inWholeSeconds.coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
