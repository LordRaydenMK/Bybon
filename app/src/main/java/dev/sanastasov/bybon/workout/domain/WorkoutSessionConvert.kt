package dev.sanastasov.bybon.workout.domain

fun WorkoutSession.convertFirstWorkSetToWarmup(exercise: WorkoutExercise): WorkoutSession =
    updateExercise(exercise.id) { current ->
        if (current.sets.size <= 1) return@updateExercise current
        val firstWorkSet = current.sets.first()
        current.copy(
            warmupSets = current.warmupSets.orEmpty() + firstWorkSet,
            sets = current.sets.drop(1),
        )
    }

fun WorkoutSession.convertLastWarmupToWorkSet(exercise: WorkoutExercise): WorkoutSession =
    updateExercise(exercise.id) { current ->
        val warmupSets = current.warmupSets ?: return@updateExercise current
        val lastWarmup = warmupSets.last()
        current.copy(
            warmupSets = warmupSets.dropLast(1).takeIf { it.isNotEmpty() },
            sets = listOf(lastWarmup) + current.sets,
        )
    }
