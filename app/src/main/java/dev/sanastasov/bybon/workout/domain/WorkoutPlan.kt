package dev.sanastasov.bybon.workout.domain

data class ExerciseSet(
    val exercise: Exercise,
    val repRange: IntRange
)

data class WorkoutPlan(
    val name: String,
    val description: String?,
    val exercises: List<ExerciseSet>
) {

    val setsByExercise = exercises.groupBy { it.exercise.id }
}

val fullBodyA = WorkoutPlan(
    "Full Body A",
    "Full body workout routine variant A",
    listOf(
        // Bench Press
        ExerciseSet(exercisesMap["bench-press-bb"]!!, 8..10),
        ExerciseSet(exercisesMap["bench-press-bb"]!!, 8..10),
        ExerciseSet(exercisesMap["bench-press-bb"]!!, 8..10),
        // Squat
        ExerciseSet(exercisesMap["squat-bb"]!!, 8..10),
        ExerciseSet(exercisesMap["squat-bb"]!!, 8..10),
        ExerciseSet(exercisesMap["squat-bb"]!!, 8..10),
        // Pull Up
        ExerciseSet(exercisesMap["pullup-assisted"]!!, 6..10),
        ExerciseSet(exercisesMap["pullup-assisted"]!!, 6..10),
        ExerciseSet(exercisesMap["pullup-assisted"]!!, 6..10),
        // Leg Curl
        ExerciseSet(exercisesMap["leg-curl"]!!, 12..14),
        ExerciseSet(exercisesMap["leg-curl"]!!, 12..14),
        ExerciseSet(exercisesMap["leg-curl"]!!, 12..14),
        // Upright row
        ExerciseSet(exercisesMap["upright-row-db"]!!, 10..14),
        ExerciseSet(exercisesMap["upright-row-db"]!!, 10..14),
        ExerciseSet(exercisesMap["upright-row-db"]!!, 10..14),
        // Skullcrusher
        ExerciseSet(exercisesMap["skullcrusher-db"]!!, 10..16),
        ExerciseSet(exercisesMap["skullcrusher-db"]!!, 10..16),
        ExerciseSet(exercisesMap["skullcrusher-db"]!!, 10..16),
    )
)

val fullBodyB = WorkoutPlan(
    "Full Body B",
    "Full body workout routine variant B",
    listOf(
        // RDL
        ExerciseSet(exercisesMap["rdl-bb"]!!, 8..10),
        ExerciseSet(exercisesMap["rdl-bb"]!!, 8..10),
        ExerciseSet(exercisesMap["rdl-bb"]!!, 8..10),
        // Incline Bench Press
        ExerciseSet(exercisesMap["incline-bench-press-db"]!!, 10..15),
        ExerciseSet(exercisesMap["incline-bench-press-db"]!!, 10..15),
        ExerciseSet(exercisesMap["incline-bench-press-db"]!!, 10..15),
        // Split Squat
        ExerciseSet(exercisesMap["split-squat-db"]!!, 8..10),
        ExerciseSet(exercisesMap["split-squat-db"]!!, 8..10),
        ExerciseSet(exercisesMap["split-squat-db"]!!, 8..10),
        // Incline Row
        ExerciseSet(exercisesMap["incline-row-db"]!!, 10..15),
        ExerciseSet(exercisesMap["incline-row-db"]!!, 10..15),
        ExerciseSet(exercisesMap["incline-row-db"]!!, 10..15),
        // Lateral Raise
        ExerciseSet(exercisesMap["lateral-raise-db"]!!, 10..15),
        ExerciseSet(exercisesMap["lateral-raise-db"]!!, 10..15),
        ExerciseSet(exercisesMap["lateral-raise-db"]!!, 10..15),
        // Incline Curl
        ExerciseSet(exercisesMap["incline-curl-db"]!!, 10..16),
        ExerciseSet(exercisesMap["incline-curl-db"]!!, 10..16),
    )
)
