package dev.sanastasov.bybon.workout.domain

import kotlin.time.Duration

data class SetRef(
    val exerciseId: String,
    val index: Int,
    val isWarmup: Boolean,
)

sealed class WorkoutState {
    data object NotStarted : WorkoutState()
    data class InProgress(
        val set: SetRef,
    ) : WorkoutState()
    data class Resting(
        val timer: RestTimer,
        val nextSet: SetRef,
    ) : WorkoutState()
    data class Completed(
        val duration: Duration,
    ) : WorkoutState()
}
