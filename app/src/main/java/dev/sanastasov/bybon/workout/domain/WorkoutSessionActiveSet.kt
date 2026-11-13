package dev.sanastasov.bybon.workout.domain

internal fun WorkoutSession.withSingleInProgressSet(): WorkoutSession {
    val open = firstNonCompletedSet()
    val draft = workoutSets.all { it.setState == SetState.NotStated }
    val aligned = open != null &&
        workoutSets.count { it.setState == SetState.InProgress } == 1 &&
        setAt(open).setState == SetState.InProgress
    return when {
        draft || aligned -> this

        open == null -> withDurationIfCompleted()

        else -> copy(exercises = exercises.map { it.clearingInProgress() }).let { cleared ->
            val nextExercise = cleared.exercises.first { it.id == open.exerciseId }
            cleared.updateExerciseSet(nextExercise, open.index, open.isWarmup) {
                it.copy(setState = SetState.InProgress)
            }
        }
    }
}

private fun WorkoutSession.firstNonCompletedSet(): SetRef? =
    exercises.firstNotNullOfOrNull { it.firstNonCompletedSet() }

private fun WorkoutExercise.firstNonCompletedSet(): SetRef? {
    val warmupIndex = warmupSets?.indexOfFirst { it.setState !is SetState.Completed } ?: -1
    if (warmupIndex >= 0) return SetRef(id, warmupIndex, isWarmup = true)
    val workIndex = sets.indexOfFirst { it.setState !is SetState.Completed }
    return if (workIndex >= 0) SetRef(id, workIndex, isWarmup = false) else null
}

private fun WorkoutSession.setAt(ref: SetRef): ExerciseSet {
    val exercise = exercises.first { it.id == ref.exerciseId }
    return if (ref.isWarmup) {
        checkNotNull(exercise.warmupSets)[ref.index]
    } else {
        exercise.sets[ref.index]
    }
}

private fun WorkoutExercise.clearingInProgress(): WorkoutExercise = copy(
    warmupSets = warmupSets?.map { it.clearingInProgress() },
    sets = sets.map { it.clearingInProgress() },
)

private fun ExerciseSet.clearingInProgress(): ExerciseSet =
    if (setState == SetState.InProgress) copy(setState = SetState.NotStated) else this
