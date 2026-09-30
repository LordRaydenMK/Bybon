package dev.sanastasov.bybon.workout.domain

private fun planned(
    exerciseId: String,
    sets: Int,
    repRange: IntRange,
    warmupSets: Int = 0,
): PlanedExercise {
    val exercise = catalogExercise(exerciseId)
    return PlanedExercise(
        exercise = exercise,
        warmupSets = warmupSets,
        sets = sets,
        repRange = repRange,
    )
}

val fullBodyA = WorkoutPlan(
    WorkoutPlanId("full-body-a"),
    "Full Body A",
    "Full body workout routine variant A",
    listOf(
        planned("bench-press", 3, 8..10, warmupSets = 3),
        planned("squat", 3, 8..10, warmupSets = 3),
        planned("assisted-pull-ups", 3, 6..10, warmupSets = 2),
        planned("seated-leg-curl", 3, 12..14),
        planned("dumbbell-upright-row", 3, 10..14),
        planned("db-skull-crusher", 3, 10..16),
    ),
)

val fullBodyB = WorkoutPlan(
    WorkoutPlanId("full-body-b"),
    "Full Body B",
    "Full body workout routine variant B",
    listOf(
        planned("romanian-deadlift", 3, 8..10, warmupSets = 2),
        planned("incline-db-press", 3, 10..15, warmupSets = 2),
        planned("bulgarian-split-squat", 3, 8..10, warmupSets = 1),
        planned("chest-supported-db-row", 3, 10..16),
        planned("lateral-raise", 3, 10..15),
        planned("incline-db-curl", 2, 10..16),
    ),
)

val upperBodyA = WorkoutPlan(
    WorkoutPlanId("upper-body-legacy"),
    "Upper Body (legacy)",
    "Upper body workout routine variant A",
    listOf(
        planned("bench-press", 3, 8..10, warmupSets = 3),
        planned("chest-supported-db-row", 3, 10..16, warmupSets = 2),
        planned("ohp", 3, 8..10, warmupSets = 2),
        planned("lat-pulldown", 3, 8..12),
        planned("lateral-raise", 3, 10..15),
        planned("incline-db-curl", 2, 10..16),
        planned("db-skull-crusher", 3, 10..16),
    ),
    isArchived = true,
)
