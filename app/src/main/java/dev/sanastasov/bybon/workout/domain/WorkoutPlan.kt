package dev.sanastasov.bybon.workout.domain

import kotlin.time.Duration
import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class WorkoutPlanId(
    val id: String,
)

data class PlanedExercise(
    val exercise: ExerciseDefinition,
    val warmupSets: Int = 0,
    val sets: Int,
    val repRange: IntRange,
    val restAfterWorkSet: Duration = exercise.defaultRest,
) {
    init {
        require(warmupSets >= 0) { "warmupSets must be >= 0" }
        require(sets >= 1) { "sets must be >= 1" }
    }
}

data class WorkoutPlan(
    val id: WorkoutPlanId,
    val name: String,
    val description: String?,
    val sets: List<PlanedExercise>,
    val isArchived: Boolean = false,
) {
    init {
        require(sets.isNotEmpty()) { "Plan must contain at least one exercise" }
    }
}
