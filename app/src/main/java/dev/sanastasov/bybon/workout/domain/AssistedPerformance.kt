package dev.sanastasov.bybon.workout.domain

import kotlin.math.roundToInt

internal val performanceSetOrder: Comparator<ExerciseSet> = compareBy(
    { it.oneRm },
    { it.effectiveLoad },
    { it.assistanceAdvantage() },
    { it.reps },
)

fun WorkoutExercise.performanceTopSet(): ExerciseSet = checkNotNull(
    sets.maxWithOrNull(performanceSetOrder),
) { "sets must contain at least one set" }

fun ExerciseSet.performanceChange(previous: ExerciseSet?): OneRmChange? {
    if (previous == null) return null
    val currentOneRm = oneRm
    val previousOneRm = previous.oneRm
    return when {
        currentOneRm != null && previousOneRm != null ->
            oneRmChange(currentOneRm, previousOneRm)

        exerciseDefinition.equipment?.isAssisted == true -> assistedChange(previous)

        else -> currentOneRm?.let { oneRmChange(it, previousOneRm) }
    }
}

private fun ExerciseSet.assistedChange(previous: ExerciseSet): OneRmChange {
    val removed = previous.assistanceAbove(this)
    val added = assistanceAbove(previous)
    return when {
        removed != null -> OneRmChange.LessAssistance(removed)
        added != null -> OneRmChange.MoreAssistance(added)
        reps > previous.reps -> OneRmChange.MoreReps
        reps < previous.reps -> OneRmChange.FewerReps
        else -> OneRmChange.Unchanged
    }
}

private fun ExerciseSet.assistanceAbove(other: ExerciseSet): Weight? {
    val mine = weight ?: return null
    val theirs = other.weight
    return if (theirs == null) mine else mine.minusOrNull(theirs)
}

private fun ExerciseSet.assistanceAdvantage(): Int {
    val logged = weight
    return when {
        exerciseDefinition.equipment?.isAssisted != true -> 0
        logged == null -> Int.MAX_VALUE
        else -> Int.MAX_VALUE - (logged.kilogramsValue * 100).roundToInt()
    }
}
