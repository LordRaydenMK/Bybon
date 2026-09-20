package dev.sanastasov.bybon.workout.domain

fun WorkoutSession.addSet(exercise: WorkoutExercise): WorkoutSession =
    updateExercise(exercise.id) { current ->
        val lastWorkSet = current.sets.lastOrNull()
        val template =
            lastWorkSet ?: current.warmupSets?.lastOrNull() ?: return@updateExercise current
        val newSetState = if (lastWorkSet?.setState == SetState.Completed ||
            (lastWorkSet == null && template.setState == SetState.Completed)
        ) {
            SetState.InProgress
        } else {
            SetState.NotStated
        }
        current.copy(
            sets = current.sets + template.copy(setState = newSetState, previous = null),
        )
    }

@Suppress("ReturnCount")
fun WorkoutSession.removeLastSet(exercise: WorkoutExercise): WorkoutSession {
    val hadInProgress = workoutSets.any { it.setState == SetState.InProgress }
    val updated = updateExercise(exercise.id) { current ->
        val lastWork = current.sets.last()
        val lastWarmup = current.warmupSets?.lastOrNull()
        when {
            current.sets.size > 1 && lastWork.setState != SetState.Completed ->
                current.copy(sets = current.sets.dropLast(1))

            lastWork.setState == SetState.Completed &&
                lastWarmup != null &&
                lastWarmup.setState != SetState.Completed ->
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

fun WorkoutSession.addExercise(exercise: ExerciseDefinition): WorkoutSession {
    check(exercises.none { it.id == exercise.id }) {
        "Exercise ${exercise.id} is already in the session"
    }
    return copy(exercises = exercises + exercise.toAddedWorkoutExercise())
}

private fun ExerciseDefinition.toAddedWorkoutExercise(): WorkoutExercise = WorkoutExercise(
    exerciseDefinition = this,
    repRange = DEFAULT_ADDED_REP_RANGE,
    warmupSets = null,
    sets = List(DEFAULT_ADDED_WORK_SETS) {
        ExerciseSet(
            exerciseDefinition = this,
            weight = Weight.kilograms(50),
            reps = DEFAULT_ADDED_REP_RANGE.first,
            setState = SetState.NotStated,
        )
    },
)

fun WorkoutSession.moveExerciseUp(exerciseId: String): WorkoutSession =
    moveExercise(exerciseId, offset = -1)

fun WorkoutSession.moveExerciseDown(exerciseId: String): WorkoutSession =
    moveExercise(exerciseId, offset = 1)

private fun WorkoutSession.moveExercise(exerciseId: String, offset: Int): WorkoutSession {
    requireExercise(exerciseId)
    val fromIndex = exercises.indexOfFirst { it.id == exerciseId }
    val toIndex = fromIndex + offset
    check(toIndex in exercises.indices) {
        val direction = if (offset < 0) "up" else "down"
        "Cannot move $exerciseId $direction"
    }
    return copy(
        exercises = exercises.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        },
    )
}

private fun WorkoutSession.requireExercise(exerciseId: String): WorkoutExercise =
    exercises.firstOrNull { it.id == exerciseId }
        ?: error("Exercise $exerciseId is not in the session")

private fun List<ExerciseSet>.nullIfEmpty(): List<ExerciseSet>? = takeIf { isNotEmpty() }
