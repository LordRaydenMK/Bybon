package dev.sanastasov.bybon.workout.domain

fun WorkoutSession.updateExercise(
    exerciseId: String,
    update: (WorkoutExercise) -> WorkoutExercise,
): WorkoutSession = copy(
    exercises = exercises.map { exercise ->
        if (exercise.id == exerciseId) {
            update(exercise)
        } else {
            exercise
        }
    },
)

internal fun WorkoutSession.clearInProgressSets(): WorkoutSession = copy(
    exercises = exercises.map { exercise ->
        exercise.copy(
            warmupSets = exercise.warmupSets?.map { it.notStartedIfInProgress() },
            sets = exercise.sets.map { it.notStartedIfInProgress() },
        )
    },
)

private fun ExerciseSet.notStartedIfInProgress(): ExerciseSet =
    if (setState == SetState.InProgress) copy(setState = SetState.NotStated) else this

fun WorkoutSession.updateExerciseSet(
    exercise: WorkoutExercise,
    setIndex: Int,
    isWarmup: Boolean = false,
    update: (ExerciseSet) -> ExerciseSet,
): WorkoutSession = updateExercise(exercise.id) { current ->
    if (isWarmup) {
        current.copy(
            warmupSets = current.warmupSets?.mapIndexed { index, set ->
                if (index == setIndex) update(set) else set
            },
        )
    } else {
        current.copy(
            sets = current.sets.mapIndexed { index, set ->
                if (index == setIndex) update(set) else set
            },
        )
    }
}

fun WorkoutSession.resetSetToPrevious(
    exercise: WorkoutExercise,
    index: Int,
    isWarmup: Boolean,
): WorkoutSession = updateExerciseSet(exercise, index, isWarmup) { it.withPreviousPerformance() }

fun WorkoutSession.resetExerciseToPrevious(exercise: WorkoutExercise): WorkoutSession =
    updateExercise(exercise.id) { current ->
        current.copy(
            warmupSets = current.warmupSets?.map { it.withPreviousPerformance() },
            sets = current.sets.map { it.withPreviousPerformance() },
        )
    }

private fun ExerciseSet.withPreviousPerformance(): ExerciseSet {
    val previous = previous ?: return this
    return copy(weight = previous.weight, reps = previous.reps)
}
