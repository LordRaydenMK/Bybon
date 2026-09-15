package dev.sanastasov.bybon.workout.domain

import kotlin.time.Duration

data class WorkoutExercise(
    val exerciseDefinition: ExerciseDefinition,
    val repRange: IntRange,
    val warmupSets: List<ExerciseSet>? = null,
    val sets: List<ExerciseSet>,
    val restAfterWorkSet: Duration = exerciseDefinition.defaultRest,
) {
    init {
        require(warmupSets == null || warmupSets.isNotEmpty()) {
            "warmupSets must be null or contain at least one set"
        }
    }

    val id: String = exerciseDefinition.id

    val orderedSets: List<ExerciseSet>
        get() = warmupSets.orEmpty() + sets

    val numberedWarmupSets: List<NumberedSet>?
        get() = warmupSets?.mapIndexed { index, set ->
            NumberedSet(set, isWarmup = true, index)
        }

    val numberedWorkSets: List<NumberedSet>
        get() = sets.mapIndexed { index, set ->
            NumberedSet(set, isWarmup = false, index)
        }

    val canRemoveSet: Boolean
        get() {
            val last = orderedSets.lastOrNull() ?: return false
            return last.setState != SetState.Completed
        }

    val hasPreviousPerformance: Boolean
        get() = orderedSets.any { it.previous != null }
}
