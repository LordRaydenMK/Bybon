package dev.sanastasov.bybon.workout.domain

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class ExerciseDefinition(
    val id: String,
    val name: String,
    val primaryMuscleGroup: MuscleGroup,
    val equipment: Equipment,
)

// Same exercises as before, on their RepDB ids. Mechanic replaces this list later.
private val compoundExerciseIds = setOf(
    "bench-press",
    "incline-bench-press",
    "incline-db-press",
    "db-bench-press",
    "chest-press-machine",
    "assisted-dips",
    "squat",
    "squat-machine",
    "romanian-deadlift",
    "deadlift",
    "bulgarian-split-squat",
    "leg-press",
    "assisted-pull-ups",
    "lat-pulldown",
    "iso-lat-row",
    "chest-supported-db-row",
    "ohp",
)

private val isolationExerciseIds = setOf(
    "incline-db-curl",
    "machine-bicep-curl",
    "db-skull-crusher",
    "triceps-press-machine",
    "lateral-raise",
    "cable-lateral-raise",
    "lateral-raise-machine",
    "dumbbell-upright-row",
    "face-pull",
)

val ExerciseDefinition.defaultRest: Duration
    get() = when (id) {
        in compoundExerciseIds -> 2.minutes
        in isolationExerciseIds -> 1.minutes
        else -> 90.seconds
    }

fun Duration.formatRestClock(): String {
    val totalSeconds = inWholeSeconds.coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
