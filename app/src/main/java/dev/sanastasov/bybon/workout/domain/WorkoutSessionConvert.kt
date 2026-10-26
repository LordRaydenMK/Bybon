package dev.sanastasov.bybon.workout.domain

fun WorkoutSession.convertFirstWorkSetToWarmup(exercise: WorkoutExercise): WorkoutSession =
    updateExercise(exercise.id) { current ->
        if (current.sets.size <= 1) return@updateExercise current
        val firstWorkSet = current.sets.first()
        current.copy(
            warmupSets = current.warmupSets.orEmpty() + firstWorkSet,
            sets = current.sets.drop(1),
        ).withPreviousMatchedTo(
            previousWarmups = current.warmupSets.orEmpty().previousPerformances(),
            previousWork = current.sets.previousPerformances(),
        )
    }

fun WorkoutSession.convertLastWarmupToWorkSet(exercise: WorkoutExercise): WorkoutSession =
    updateExercise(exercise.id) { current ->
        val warmupSets = current.warmupSets ?: return@updateExercise current
        val lastWarmup = warmupSets.last()
        current.copy(
            warmupSets = warmupSets.dropLast(1).takeIf { it.isNotEmpty() },
            sets = listOf(lastWarmup) + current.sets,
        ).withPreviousMatchedTo(
            previousWarmups = warmupSets.previousPerformances(),
            previousWork = current.sets.previousPerformances(),
        )
    }

private fun List<ExerciseSet>.previousPerformances(): List<PreviousSetPerformance?> =
    map { it.previous }

private fun WorkoutExercise.withPreviousMatchedTo(
    previousWarmups: List<PreviousSetPerformance?>,
    previousWork: List<PreviousSetPerformance?>,
): WorkoutExercise = copy(
    warmupSets = warmupSets?.mapIndexed { index, set ->
        set.copy(previous = previousWarmups.getOrNull(index))
    },
    sets = sets.mapIndexed { index, set ->
        set.copy(previous = previousWork.getOrNull(index))
    },
)
