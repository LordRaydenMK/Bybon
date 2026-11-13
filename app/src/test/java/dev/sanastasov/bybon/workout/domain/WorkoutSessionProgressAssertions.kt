package dev.sanastasov.bybon.workout.domain

internal fun WorkoutSession.completeExerciseAt(exerciseIndex: Int): WorkoutSession {
    val warmed = (0 until (exercises[exerciseIndex].warmupSets?.size ?: 0))
        .fold(this) { session, index ->
            session.completeSet(session.exercises[exerciseIndex], index, isWarmup = true)
        }
    return warmed.exercises[exerciseIndex].sets.indices.fold(warmed) { session, index ->
        session.completeSet(session.exercises[exerciseIndex], index)
    }
}

internal fun WorkoutSession.assertOnlyInProgress(
    exerciseId: String,
    index: Int,
    isWarmup: Boolean,
) {
    assert(workoutSets.count { it.setState == SetState.InProgress } == 1)
    val current = checkNotNull(currentExerciseIndex)
    assert(exercises[current].id == exerciseId)
    assert(exercises.take(current).all { it.state == ExerciseState.Completed })
    val exercise = exercises[current]
    val active = if (isWarmup) {
        checkNotNull(exercise.warmupSets)[index]
    } else {
        exercise.sets[index]
    }
    assert(active.setState == SetState.InProgress)
    assert(
        exercise.orderedSets.first { it.setState !is SetState.Completed }.setState ==
            SetState.InProgress,
    )
}
