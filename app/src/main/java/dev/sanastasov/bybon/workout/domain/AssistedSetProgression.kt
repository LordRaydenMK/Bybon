package dev.sanastasov.bybon.workout.domain

internal fun ExerciseSet.adjustAssisted(
    repRange: IntRange,
    increment: Weight,
    increase: Boolean,
): ExerciseSet {
    if (weight == null && increase) return this
    return adjustLoggedWeight(
        repRange = repRange,
        increment = increment,
        increase = increase,
        decreasingLoad = increase,
        start = weight,
    )
}
