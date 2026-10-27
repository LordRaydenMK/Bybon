package dev.sanastasov.bybon.workout.domain

data class NumberedSet(
    val set: ExerciseSet,
    val isWarmup: Boolean,
    val index: Int,
) {
    val workSetNumber: Int?
        get() = if (isWarmup) null else index + 1

    val warmupSetNumber: Int?
        get() = if (isWarmup) index + 1 else null

    val setNumber: Int
        get() = index + 1
}
