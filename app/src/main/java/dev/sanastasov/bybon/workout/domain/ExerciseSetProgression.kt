package dev.sanastasov.bybon.workout.domain

fun ExerciseSet.adjust(repRange: IntRange, increment: Weight?, increase: Boolean): ExerciseSet =
    when {
        increase && reps < repRange.last -> copy(reps = reps + 1)

        !increase && reps > repRange.first -> copy(reps = reps - 1)

        increment == null -> this

        exerciseDefinition.equipment?.isAssisted == true ->
            adjustAssisted(repRange, increment, increase)

        weight == null -> this

        else -> adjustLoggedWeight(
            repRange = repRange,
            increment = increment,
            increase = increase,
            decreasingLoad = !increase,
            start = weight,
        )
    }

internal fun ExerciseSet.followFirstWorkSet(
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

        else -> withWeightPreservingOneRm(
            newWeight,
            preferredRange = expandedRange,
            fallbackRange = expandedRange,
            increase = increase,
        ) ?: copy(weight = newWeight)
    }
}

internal fun ExerciseSet.adjustLoggedWeight(
    repRange: IntRange,
    increment: Weight,
    increase: Boolean,
    decreasingLoad: Boolean,
    start: Weight?,
): ExerciseSet {
    val expandedRange = repRange.expanded()
    return loggedWeightSteps(start, increment, decreasingLoad).firstNotNullOfOrNull { newWeight ->
        withWeightPreservingOneRm(
            newWeight,
            preferredRange = repRange,
            fallbackRange = expandedRange,
            increase = increase,
        )
    } ?: this
}

private fun loggedWeightSteps(
    start: Weight?,
    increment: Weight,
    decreasing: Boolean,
): List<Weight?> {
    val steps = mutableListOf<Weight?>()
    var current = start
    repeat(64) {
        current = if (decreasing) {
            current?.minusOrNull(increment)
        } else if (current == null) {
            increment
        } else {
            current + increment
        }
        steps.add(current)
        if (decreasing && current == null) return steps
    }
    return steps
}

private fun ExerciseSet.withWeightPreservingOneRm(
    newWeight: Weight?,
    preferredRange: IntRange,
    fallbackRange: IntRange,
    increase: Boolean,
): ExerciseSet? {
    val currentOneRm = oneRm ?: return copy(weight = newWeight)
    fun pick(range: IntRange): Int? {
        val candidates = range.filter { candidateReps ->
            val candidateOneRm = oneRmFor(newWeight, candidateReps) ?: return@filter false
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
