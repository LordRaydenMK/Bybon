package dev.sanastasov.bybon.workout.domain

fun WorkoutSession.addSet(exercise: WorkoutExercise): WorkoutSession =
    updateExercise(exercise.id) { current ->
        val template = current.sets.lastOrNull()
            ?: current.warmupSets?.lastOrNull()
            ?: return@updateExercise current
        current.copy(
            sets = current.sets + template.copy(setState = SetState.NotStated, previous = null),
        )
    }.withSingleInProgressSet()

@Suppress("ReturnCount")
fun WorkoutSession.removeLastSet(exercise: WorkoutExercise): WorkoutSession {
    val hadInProgress = workoutSets.any { it.setState == SetState.InProgress }
    val updated = updateExercise(exercise.id) { current ->
        val lastWork = current.sets.last()
        val lastWarmup = current.warmupSets?.lastOrNull()
        when {
            current.sets.size > 1 && lastWork.setState !is SetState.Completed ->
                current.copy(sets = current.sets.dropLast(1))

            lastWork.setState is SetState.Completed &&
                lastWarmup != null &&
                lastWarmup.setState !is SetState.Completed ->
                current.copy(
                    warmupSets = current.warmupSets.orEmpty().dropLast(1).nullIfEmpty(),
                )

            else -> current
        }
    }
    if (!hadInProgress || updated.workoutSets.any { it.setState == SetState.InProgress }) {
        return updated
    }
    val next = updated.firstNotStartedSet() ?: return updated
    val nextExercise = updated.exercises.first { it.id == next.exerciseId }
    return updated.updateExerciseSet(nextExercise, next.index, next.isWarmup) {
        it.copy(setState = SetState.InProgress)
    }
}

fun WorkoutSession.updateWeight(
    exercise: WorkoutExercise,
    setIndex: Int,
    weight: Weight?,
    isWarmup: Boolean = false,
): WorkoutSession {
    if (isWarmup) {
        return updateExerciseSet(exercise, setIndex, isWarmup = true) {
            it.copy(weight = weight)
        }
    }
    return updateExercise(exercise.id) { current ->
        current.copy(
            sets = current.sets.mapIndexed { index, set ->
                when {
                    index == setIndex -> set.copy(weight = weight)
                    weight != null && index > setIndex -> set.copy(weight = weight)
                    else -> set
                }
            },
        )
    }
}

fun WorkoutSession.updateReps(
    exercise: WorkoutExercise,
    setIndex: Int,
    count: Int,
    isWarmup: Boolean = false,
): WorkoutSession = updateExerciseSet(exercise, setIndex, isWarmup) {
    it.copy(reps = count)
}

private fun List<ExerciseSet>.nullIfEmpty(): List<ExerciseSet>? = takeIf { it.isNotEmpty() }
