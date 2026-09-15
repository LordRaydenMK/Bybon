package dev.sanastasov.bybon.workout.ui

enum class ExerciseCardMode {
    Overview,
    Session,
}

sealed class ExerciseCardEvent {
    data object OnIncrease : ExerciseCardEvent()
    data object OnDecrease : ExerciseCardEvent()
    data object OnResetAllSets : ExerciseCardEvent()
    data class OnResetSet(
        val index: Int,
        val isWarmup: Boolean,
    ) : ExerciseCardEvent()

    data class OnWeightUpdated(
        val weight: String,
        val index: Int,
        val isWarmup: Boolean,
    ) : ExerciseCardEvent()

    data class OnRepsUpdated(
        val reps: String,
        val index: Int,
        val isWarmup: Boolean,
    ) : ExerciseCardEvent()

    data object OnAddSet : ExerciseCardEvent()
    data object OnRemoveLastSet : ExerciseCardEvent()
    data object OnConvertToWarmup : ExerciseCardEvent()
    data object OnConvertToWorkSet : ExerciseCardEvent()
    data class OnCompleteSet(
        val index: Int,
        val isWarmup: Boolean,
    ) : ExerciseCardEvent()
}
