package dev.sanastasov.bybon.workout.domain

private val warmupReps = listOf(8, 4, 3)

private fun planned(
    exerciseId: String,
    sets: Int,
    repRange: IntRange,
    warmupCount: Int = 0,
): PlanedExercise {
    val exercise = exercisesMap.getValue(exerciseId)
    return PlanedExercise(
        exercise = exercise,
        sets = sets,
        repRange = repRange,
        warmupSets = List(warmupCount) { index ->
            PlanedWarmupSet(
                weight = exercise.equipment.defaultWarmupWeight,
                reps = warmupReps.getOrElse(index) { warmupReps.last() },
            )
        },
    )
}

val fullBodyA = WorkoutPlan(
    WorkoutPlanId("full-body-a"),
    "Full Body A",
    "Full body workout routine variant A",
    listOf(
        planned("bench-press-bb", 3, 8..10, warmupCount = 3),
        planned("squat-bb", 3, 8..10, warmupCount = 3),
        planned("pullup-assisted", 3, 6..10, warmupCount = 2),
        planned("leg-curl", 3, 12..14),
        planned("upright-row-db", 3, 10..14),
        planned("skullcrusher-db", 3, 10..16),
    ),
)

val fullBodyB = WorkoutPlan(
    WorkoutPlanId("full-body-b"),
    "Full Body B",
    "Full body workout routine variant B",
    listOf(
        planned("rdl-bb", 3, 8..10, warmupCount = 2),
        planned("incline-bench-press-db", 3, 10..15, warmupCount = 2),
        planned("split-squat-db", 3, 8..10, warmupCount = 1),
        planned("incline-row-db", 3, 10..16),
        planned("lateral-raise-db", 3, 10..15),
        planned("incline-curl-db", 2, 10..16),
    ),
)
