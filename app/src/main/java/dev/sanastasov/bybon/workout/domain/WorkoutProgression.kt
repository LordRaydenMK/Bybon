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

private fun WorkoutExercise.adjust(increase: Boolean): WorkoutExercise {
    val originalFirst = sets.firstOrNull() ?: return this
    val increment = exerciseDefinition.equipment?.weightIncrement
    val adjustedFirst = originalFirst.adjust(repRange, increment, increase)
    return copy(
        sets = listOf(adjustedFirst) + sets.drop(1).map { set ->
            set.followFirstWorkSet(originalFirst, adjustedFirst, repRange, increase)
        },
    )
}

fun ExerciseSet.adjust(repRange: IntRange, increment: Weight?, increase: Boolean): ExerciseSet {
    val load = weight
    return when {
        increase && reps < repRange.last -> copy(reps = reps + 1)

        !increase && reps > repRange.first -> copy(reps = reps - 1)

        increment == null || load == null -> this

        else -> {
            val expandedRange = repRange.expanded()
            generateSequence(
                if (increase) load + increment else load.minusOrNull(increment),
            ) { current ->
                if (increase) current + increment else current.minusOrNull(increment)
            }.take(64).firstNotNullOfOrNull { newWeight ->
                withWeightPreservingOneRm(
                    newWeight,
                    preferredRange = repRange,
                    fallbackRange = expandedRange,
                    increase = increase,
                )
            } ?: this
        }
    }
}

private fun ExerciseSet.followFirstWorkSet(
    originalFirst: ExerciseSet,
    adjustedFirst: ExerciseSet,
    repRange: IntRange,
    increase: Boolean,
): ExerciseSet {
    val expandedRange = repRange.expanded()
    val newWeight = adjustedFirst.weight
    return when {
        newWeight == originalFirst.weight -> {
            val delta = adjustedFirst.reps - originalFirst.reps
            copy(reps = (reps + delta).coerceIn(expandedRange))
        }

        newWeight == null -> copy(weight = null)

        else -> withWeightPreservingOneRm(
            newWeight,
            preferredRange = expandedRange,
            fallbackRange = expandedRange,
            increase = increase,
        ) ?: copy(weight = newWeight)
    }
}

private fun ExerciseSet.withWeightPreservingOneRm(
    newWeight: Weight,
    preferredRange: IntRange,
    fallbackRange: IntRange,
    increase: Boolean,
): ExerciseSet? {
    val currentOneRm = oneRm ?: return copy(weight = newWeight)
    fun pick(range: IntRange): Int? {
        val candidates = range.filter { candidateReps ->
            val candidateOneRm = estimatedOneRm(newWeight, candidateReps)
            if (increase) candidateOneRm > currentOneRm else candidateOneRm < currentOneRm
        }
        return if (increase) candidates.minOrNull() else candidates.maxOrNull()
    }
    return (pick(preferredRange) ?: pick(fallbackRange))?.let { reps ->
        copy(weight = newWeight, reps = reps)
    }
}

private fun IntRange.expanded(amount: Int = 2): IntRange =
    (first - amount).coerceAtLeast(1)..(last + amount)
