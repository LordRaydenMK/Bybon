package dev.sanastasov.bybon.workout.domain

fun WorkoutSession.completeSet(
    exercise: WorkoutExercise,
    setIndex: Int,
    isWarmup: Boolean = false,
): WorkoutSession {
    val next = nextSetAfter(exercise.id, setIndex, isWarmup)
    val updated = updateExerciseSet(exercise, setIndex, isWarmup) {
        it.copy(setState = SetState.Completed)
    }
    return if (next != null) {
        val nextExercise = updated.exercises.first { it.id == next.exerciseId }
        updated.updateExerciseSet(nextExercise, next.index, next.isWarmup) {
            it.copy(setState = SetState.InProgress)
        }
    } else {
        updated
    }
}

data class SetRef(
    val exerciseId: String,
    val index: Int,
    val isWarmup: Boolean,
)

private fun WorkoutSession.nextSetAfter(
    exerciseId: String,
    setIndex: Int,
    isWarmup: Boolean,
): SetRef? {
    val exercise = exercises.first { it.id == exerciseId }
    val nextInExercise = if (isWarmup) {
        nextSetAfterWarmup(exercise, setIndex)
    } else {
        nextSetAfterWork(exercise, setIndex)
    }
    return nextInExercise ?: firstSetOfNextExercise(exerciseId)
}

private fun nextSetAfterWarmup(exercise: WorkoutExercise, setIndex: Int): SetRef? {
    val warmupSets = exercise.warmupSets
    return when {
        warmupSets != null && setIndex < warmupSets.lastIndex ->
            SetRef(exercise.id, setIndex + 1, isWarmup = true)

        exercise.sets.isNotEmpty() ->
            SetRef(exercise.id, 0, isWarmup = false)

        else -> null
    }
}

private fun nextSetAfterWork(exercise: WorkoutExercise, setIndex: Int): SetRef? =
    if (setIndex < exercise.sets.lastIndex) {
        SetRef(exercise.id, setIndex + 1, isWarmup = false)
    } else {
        null
    }

private fun WorkoutSession.firstSetOfNextExercise(exerciseId: String): SetRef? {
    val nextExercise = exercises.getOrNull(exercises.indexOfFirst { it.id == exerciseId } + 1)
        ?: return null
    return when {
        nextExercise.warmupSets != null -> SetRef(nextExercise.id, 0, isWarmup = true)
        nextExercise.sets.isNotEmpty() -> SetRef(nextExercise.id, 0, isWarmup = false)
        else -> null
    }
}

fun WorkoutSession.firstNotStartedSet(): SetRef? =
    exercises.firstNotNullOfOrNull { exercise ->
        val warmupIndex = exercise.warmupSets?.indexOfFirst { it.setState == SetState.NotStated }
        when {
            warmupIndex != null && warmupIndex >= 0 ->
                SetRef(exercise.id, warmupIndex, isWarmup = true)

            else -> {
                val workIndex = exercise.sets.indexOfFirst { it.setState == SetState.NotStated }
                if (workIndex >= 0) SetRef(exercise.id, workIndex, isWarmup = false) else null
            }
        }
    }
