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
        require(sets.isNotEmpty()) { "sets must contain at least one set" }
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
            val lastWork = sets.last()
            if (sets.size > 1 && lastWork.setState != SetState.Completed) return true
            if (lastWork.setState != SetState.Completed) return false
            return warmupSets?.lastOrNull()?.setState?.let { it != SetState.Completed } == true
        }

    val hasPreviousPerformance: Boolean
        get() = orderedSets.any { it.previous != null }

    val state: ExerciseState
        get() {
            val allSets = orderedSets
            return when {
                allSets.all { it.setState == SetState.Completed } ->
                    ExerciseState.Completed

                allSets.all { it.setState == SetState.NotStated } ->
                    ExerciseState.NotStarted

                else -> ExerciseState.InProgress
            }
        }
}
