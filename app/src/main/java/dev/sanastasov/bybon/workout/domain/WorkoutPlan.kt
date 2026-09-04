package dev.sanastasov.bybon.workout.domain

data class ExerciseForReps(
    val exercise: Exercise,
    val sets: Int,
    val repRange: IntRange
)

data class WorkoutPlan(
    val name: String,
    val description: String?,
    val exercises: List<ExerciseForReps>
)

val fullBodyA = WorkoutPlan(
    "Full Body A",
    null,
    listOf(
        ExerciseForReps(exercisesMap["bench-press-bb"]!!, 3, 8..10),
        ExerciseForReps(exercisesMap["squat-bb"]!!, 3, 8..10),
        ExerciseForReps(exercisesMap["pullup-assisted"]!!, 3, 6..10),
        ExerciseForReps(exercisesMap["leg-curl"]!!, 3, 12..14),
        ExerciseForReps(exercisesMap["upright-row-db"]!!, 3, 10..14),
        ExerciseForReps(exercisesMap["skullcrusher-db"]!!, 3, 10..16),
    )
)

val fullBodyB = WorkoutPlan(
    "Full Body B",
    null,
    listOf(
        ExerciseForReps(exercisesMap["rdl-bb"]!!, 3, 8..10),
        ExerciseForReps(exercisesMap["incline-row-db"]!!, 3, 10..15),
        ExerciseForReps(exercisesMap["split-squat-db"]!!, 3, 8..10),
        ExerciseForReps(exercisesMap["incline-row-db"]!!, 3, 10..15),
        ExerciseForReps(exercisesMap["lateral-raise-db"]!!, 3, 10..15),
        ExerciseForReps(exercisesMap["incline-curl-db"]!!, 2, 10..16),
    )
)
