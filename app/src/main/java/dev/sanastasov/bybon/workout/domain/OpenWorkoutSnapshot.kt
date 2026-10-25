package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime
import kotlin.time.Duration

data class OpenWorkoutSnapshot(
    val session: WorkoutSession,
    val elapsed: Duration,
    val restRemaining: Duration?,
) {
    init {
        when (val state = session.state) {
            is WorkoutState.InProgress ->
                require(restRemaining == null)

            is WorkoutState.Resting ->
                requireNotNull(restRemaining)

            else ->
                error("open snapshot requires InProgress or Resting, was $state")
        }
    }

    val shouldActivateNextSet: Boolean
        get() = restRemaining == Duration.ZERO
}

fun WorkoutSession.toOpenSnapshot(now: LocalDateTime): OpenWorkoutSnapshot? {
    if (state !is WorkoutState.InProgress && state !is WorkoutState.Resting) return null
    return OpenWorkoutSnapshot(
        session = this,
        elapsed = elapsedSince(startedAt, now),
        restRemaining = (state as? WorkoutState.Resting)?.timer?.remaining(now),
    )
}
