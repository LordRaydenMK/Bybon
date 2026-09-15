package dev.sanastasov.bybon.workout.domain

import kotlin.time.Duration

sealed class WorkoutState {
    data object NotStarted : WorkoutState()
    data object InProgress : WorkoutState()
    data class Completed(
        val duration: Duration,
    ) : WorkoutState()
}
