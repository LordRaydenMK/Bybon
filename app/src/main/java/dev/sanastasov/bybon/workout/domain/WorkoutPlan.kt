package dev.sanastasov.bybon.workout.domain

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@JvmInline
@Serializable
value class WorkoutPlanId(val id: String)

@JvmInline
value class Weight(private val value: Int) {

    companion object {

        fun kilograms(value: Int): Weight = Weight(value * 10)

        fun kilograms(value: Float): Weight = Weight((value * 10).roundToInt())
    }
}

data class ExerciseSet(
    val exerciseDefinition: ExerciseDefinition,
    val repRange: IntRange
)

data class WorkoutExercise(
    val id: String,
    val exerciseDefinition: ExerciseDefinition,
)

data class PlanedSet(
    val exercise: ExerciseDefinition,
    val sets: Int,
    val repRange: IntRange
)

data class WorkoutPlan(
    val id: WorkoutPlanId,
    val name: String,
    val description: String?,
    val sets: List<PlanedSet>
)

val fullBodyA = WorkoutPlan(
    WorkoutPlanId("full-body-a"),
    "Full Body A",
    "Full body workout routine variant A",
    listOf(
        PlanedSet(exercisesMap["bench-press-bb"]!!, 3, 8..10),
        PlanedSet(exercisesMap["squat-bb"]!!, 3, 8..10),
        PlanedSet(exercisesMap["pullup-assisted"]!!, 3, 6..10),
        PlanedSet(exercisesMap["leg-curl"]!!, 3, 12..14),
        PlanedSet(exercisesMap["upright-row-db"]!!, 3, 10..14),
        PlanedSet(exercisesMap["skullcrusher-db"]!!, 3, 10..16),
    )
)

val fullBodyB = WorkoutPlan(
    WorkoutPlanId("full-body-b"),
    "Full Body B",
    "Full body workout routine variant B",
    listOf(
        PlanedSet(exercisesMap["rdl-bb"]!!, 3, 8..10),
        PlanedSet(exercisesMap["incline-bench-press-db"]!!, 3, 10..15),
        PlanedSet(exercisesMap["split-squat-db"]!!, 3, 8..10),
        PlanedSet(exercisesMap["incline-row-db"]!!, 3, 10..16),
        PlanedSet(exercisesMap["lateral-raise-db"]!!, 3, 10..15),
        PlanedSet(exercisesMap["incline-curl-db"]!!, 2, 10..16),
    ),
)
