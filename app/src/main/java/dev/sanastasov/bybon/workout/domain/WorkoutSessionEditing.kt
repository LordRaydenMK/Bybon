package dev.sanastasov.bybon.workout.domain

fun WorkoutSession.addSet(exercise: WorkoutExercise): WorkoutSession {
    val current = exercises.first { it.id == exercise.id }
    val lastWorkSet = current.sets.lastOrNull()
    val template = lastWorkSet ?: current.warmupSets?.lastOrNull()
    val startNewSet = lastWorkSet?.setState == SetState.Completed ||
        (lastWorkSet == null && template?.setState == SetState.Completed)
    val base = if (startNewSet) clearInProgressSets() else this
    return template?.let { templateSet ->
        base.updateExercise(exercise.id) { ex ->
            ex.copy(
                sets = ex.sets + templateSet.copy(
                    setState = if (startNewSet) SetState.InProgress else SetState.NotStated,
                    previous = null,
                ),
            )
        }
    } ?: this
}

fun WorkoutSession.removeLastSet(exercise: WorkoutExercise): WorkoutSession {
    val hadInProgress = workoutSets.any { it.setState == SetState.InProgress }
    val updated = updateExercise(exercise.id) { current ->
        when {
            current.sets.lastOrNull()?.let { it.setState != SetState.Completed } == true ->
                current.copy(sets = current.sets.dropLast(1))

            current.warmupSets?.lastOrNull()?.let { it.setState != SetState.Completed } == true ->
                current.copy(
                    warmupSets = current.warmupSets.orEmpty().dropLast(1).nullIfEmpty(),
                )

            else -> current
        }
    }
    val next = updated.firstNotStartedSet()
    val shouldPromote = hadInProgress &&
        updated.workoutSets.none { it.setState == SetState.InProgress }
    return next.takeIf { shouldPromote }?.let { nextSet ->
        val nextExercise = updated.exercises.first { it.id == nextSet.exerciseId }
        updated.updateExerciseSet(nextExercise, nextSet.index, nextSet.isWarmup) {
            it.copy(setState = SetState.InProgress)
        }
    } ?: updated
}

fun WorkoutSession.convertFirstWorkSetToWarmup(exercise: WorkoutExercise): WorkoutSession =
    updateExercise(exercise.id) { current ->
        val firstWorkSet = current.sets.firstOrNull() ?: return@updateExercise current
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
            warmupSets = warmupSets.dropLast(1).nullIfEmpty(),
            sets = listOf(lastWarmup) + current.sets,
        )
    }

fun WorkoutSession.updateWeight(
    exercise: WorkoutExercise,
    setIndex: Int,
    weight: Weight,
    isWarmup: Boolean = false,
): WorkoutSession = updateExerciseSet(exercise, setIndex, isWarmup) {
    it.copy(weight = weight)
}

fun WorkoutSession.updateReps(
    exercise: WorkoutExercise,
    setIndex: Int,
    count: Int,
    isWarmup: Boolean = false,
): WorkoutSession = updateExerciseSet(exercise, setIndex, isWarmup) {
    it.copy(reps = count)
}

private fun List<ExerciseSet>.nullIfEmpty(): List<ExerciseSet>? = takeIf { isNotEmpty() }
