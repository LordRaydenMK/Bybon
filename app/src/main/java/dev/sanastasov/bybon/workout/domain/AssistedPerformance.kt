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

fun ExerciseSet.performanceChange(previous: ExerciseSet): OneRmChange? {
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
        removed != null -> OneRmChange.Improved(OneRmChange.How.Assistance(removed))
        added != null -> OneRmChange.Declined(OneRmChange.How.Assistance(added))
        reps > previous.reps -> OneRmChange.Improved(OneRmChange.How.Reps)
        reps < previous.reps -> OneRmChange.Declined(OneRmChange.How.Reps)
        else -> OneRmChange.Maintained
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
