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

private val compoundExerciseIds = setOf(
    "bench-press-bb",
    "incline-bench-press-bb",
    "incline-bench-press-db",
    "bench-press-db",
    "chest-press-machine",
    "chest-dip",
    "squat-bb",
    "squat-machine",
    "rdl-bb",
    "deadlift-barbell",
    "split-squat-db",
    "leg-press",
    "pullup-assisted",
    "lat-pull-down",
    "iso-lat-row",
    "incline-row-db",
    "overhead-press-bb",
)

private val isolationExerciseIds = setOf(
    "incline-curl-db",
    "biceps-curl-machine",
    "skullcrusher-db",
    "triceps-press-machine",
    "lateral-raise-db",
    "lateral-raise-cable",
    "lateral-raise-machine",
    "upright-row-db",
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
