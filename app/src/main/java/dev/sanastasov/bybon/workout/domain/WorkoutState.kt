package dev.sanastasov.bybon.workout.domain

import kotlin.time.Duration

sealed class WorkoutState {
    enum class Kind {
        Draft,
        InProgress,
        Completed,
    }

    data object NotStarted : WorkoutState()
    data object InProgress : WorkoutState()
    data class Completed(
        val duration: Duration,
    ) : WorkoutState()
}
