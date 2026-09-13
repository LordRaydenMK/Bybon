package dev.sanastasov.bybon.workout.domain

val fullBodyA = WorkoutPlan(
    WorkoutPlanId("full-body-a"),
    "Full Body A",
    "Full body workout routine variant A",
    listOf(
        PlanedExercise(exercisesMap["bench-press-bb"]!!, 3, 8..10),
        PlanedExercise(exercisesMap["squat-bb"]!!, 3, 8..10),
        PlanedExercise(exercisesMap["pullup-assisted"]!!, 3, 6..10),
        PlanedExercise(exercisesMap["leg-curl"]!!, 3, 12..14),
        PlanedExercise(exercisesMap["upright-row-db"]!!, 3, 10..14),
        PlanedExercise(exercisesMap["skullcrusher-db"]!!, 3, 10..16)
    )
)

val fullBodyB = WorkoutPlan(
    WorkoutPlanId("full-body-b"),
    "Full Body B",
    "Full body workout routine variant B",
    listOf(
        PlanedExercise(exercisesMap["rdl-bb"]!!, 3, 8..10),
        PlanedExercise(exercisesMap["incline-bench-press-db"]!!, 3, 10..15),
        PlanedExercise(exercisesMap["split-squat-db"]!!, 3, 8..10),
        PlanedExercise(exercisesMap["incline-row-db"]!!, 3, 10..16),
        PlanedExercise(exercisesMap["lateral-raise-db"]!!, 3, 10..15),
        PlanedExercise(exercisesMap["incline-curl-db"]!!, 2, 10..16)
    )
)
