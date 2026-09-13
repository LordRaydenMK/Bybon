package dev.sanastasov.bybon.workout.domain

data class NumberedSet(
    val set: ExerciseSet,
    val isWarmup: Boolean,
    val index: Int,
) {
    val workSetNumber: Int?
        get() = if (isWarmup) null else index + 1
}
