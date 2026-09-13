package dev.sanastasov.bybon.workout.domain

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class WorkoutPlanId(
    val id: String
)

data class PlanedWarmupSet(
    val weight: Weight,
    val reps: Int,
)

data class PlanedExercise(
    val exercise: ExerciseDefinition,
    val sets: Int,
    val repRange: IntRange,
    val warmupSets: List<PlanedWarmupSet> = emptyList(),
)

data class WorkoutPlan(
    val id: WorkoutPlanId,
    val name: String,
    val description: String?,
    val sets: List<PlanedExercise>,
)
