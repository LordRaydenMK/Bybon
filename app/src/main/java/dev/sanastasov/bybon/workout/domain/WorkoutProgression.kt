package dev.sanastasov.bybon.workout.domain

fun WorkoutSession.adjustAll(increase: Boolean): WorkoutSession =
    copy(exercises = exercises.map { it.adjust(increase) })

fun WorkoutSession.adjustExercise(exercise: WorkoutExercise, increase: Boolean): WorkoutSession =
    updateExercise(exercise.id) { it.adjust(increase) }

fun WorkoutSession.resetTo(previous: WorkoutSession): WorkoutSession = previous

fun WorkoutSession.resetExercise(
    exercise: WorkoutExercise,
    previous: WorkoutSession,
): WorkoutSession {
    val restored = previous.exercises.firstOrNull { it.id == exercise.id } ?: return this
    return updateExercise(exercise.id) { restored }
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

private fun WorkoutExercise.adjust(increase: Boolean): WorkoutExercise {
    val originalFirst = sets.firstOrNull() ?: return this
    val increment = exerciseDefinition.equipment.weightIncrement
    val adjustedFirst = originalFirst.adjust(repRange, increment, increase)
    return copy(
        sets = listOf(adjustedFirst) + sets.drop(1).map { set ->
            set.followFirstWorkSet(originalFirst, adjustedFirst, repRange, increase)
        },
    )
}

@Suppress("ReturnCount")
internal fun ExerciseSet.adjust(
    repRange: IntRange,
    increment: Weight,
    increase: Boolean,
): ExerciseSet {
    val expandedRange = repRange.expanded()
    if (increase) {
        if (reps < repRange.last) {
            return copy(reps = reps + 1)
        }
        if (increment.kilogramsValue <= 0f) return this
        var newWeight = weight + increment
        repeat(64) {
            withWeightPreservingOneRm(
                newWeight,
                preferredRange = repRange,
                fallbackRange = expandedRange,
                increase = true,
            )?.let { return it }
            newWeight += increment
        }
        return this
    }
    if (reps > repRange.first) {
        return copy(reps = reps - 1)
    }
    if (increment.kilogramsValue <= 0f || weight.kilogramsValue <= 0f) return this
    var newWeight = weight - increment
    repeat(64) {
        if (newWeight.kilogramsValue <= 0f) return this
        withWeightPreservingOneRm(
            newWeight,
            preferredRange = repRange,
            fallbackRange = expandedRange,
            increase = false,
        )?.let { return it }
        newWeight -= increment
    }
    return this
}

private fun ExerciseSet.followFirstWorkSet(
    originalFirst: ExerciseSet,
    adjustedFirst: ExerciseSet,
    repRange: IntRange,
    increase: Boolean,
): ExerciseSet {
    val expandedRange = repRange.expanded()
    if (adjustedFirst.weight == originalFirst.weight) {
        val delta = adjustedFirst.reps - originalFirst.reps
        return copy(reps = (reps + delta).coerceIn(expandedRange))
    }
    return withWeightPreservingOneRm(
        adjustedFirst.weight,
        preferredRange = expandedRange,
        fallbackRange = expandedRange,
        increase = increase,
    ) ?: copy(weight = adjustedFirst.weight)
}

private fun ExerciseSet.withWeightPreservingOneRm(
    newWeight: Weight,
    preferredRange: IntRange,
    fallbackRange: IntRange,
    increase: Boolean,
): ExerciseSet? {
    val currentOneRm = oneRm
    fun pick(range: IntRange): Int? {
        val candidates = range.filter { candidateReps ->
            val candidateOneRm = oneRmOrNull(newWeight, candidateReps) ?: return@filter false
            if (increase) {
                currentOneRm == null || candidateOneRm > currentOneRm
            } else {
                currentOneRm == null || candidateOneRm < currentOneRm
            }
        }
        return if (increase) candidates.minOrNull() else candidates.maxOrNull()
    }
    val reps = pick(preferredRange) ?: pick(fallbackRange) ?: return null
    return copy(weight = newWeight, reps = reps)
}

private fun IntRange.expanded(amount: Int = 2): IntRange =
    (first - amount).coerceAtLeast(1)..(last + amount)
