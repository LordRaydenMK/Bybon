package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import org.junit.Test

class OpenWorkoutSnapshotTest {

    private val startedAt = LocalDateTime.of(2026, 10, 24, 12, 0, 0)

    @Test
    fun `toOpenSnapshot is null for not started and completed sessions`() {
        val overview = fullBodyA.toOverviewSession()
        val completed = fullBodyA.toWorkoutSession(startedAt = startedAt).let { session ->
            session.exercises.indices.fold(session) { current, exerciseIndex ->
                completeAllSets(current, exerciseIndex, startedAt.plusMinutes(40))
            }
        }

        assert(overview.toOpenSnapshot(startedAt) == null)
        assert(completed.state is WorkoutState.Completed)
        assert(completed.toOpenSnapshot(startedAt.plusMinutes(40)) == null)
    }

    @Test
    fun `in progress snapshot has elapsed clock and no rest remaining`() {
        val now = startedAt.plusMinutes(5)
        val session = fullBodyA.toWorkoutSession(startedAt = startedAt)
        val snapshot = session.toOpenSnapshot(now)

        assert(snapshot != null)
        assert(session.state is WorkoutState.InProgress)
        assert(snapshot!!.elapsed == 5.minutes)
        assert(snapshot.restRemaining == null)
        assert(!snapshot.shouldActivateNextSet)
    }

    @Test
    fun `resting snapshot remaining zero is still resting until startWorkout`() {
        val completedAt = startedAt.plusMinutes(4)
        val resting = completeAllWarmups(
            fullBodyA.toWorkoutSession(startedAt = startedAt),
            exerciseIndex = 0,
            now = completedAt,
        )
        val expiredAt = completedAt.plusMinutes(2)
        val snapshot = resting.toOpenSnapshot(expiredAt)

        assert(resting.state is WorkoutState.Resting)
        assert(snapshot != null)
        assert(snapshot!!.restRemaining == Duration.ZERO)
        assert(snapshot.shouldActivateNextSet)
        assert(snapshot.session.state is WorkoutState.Resting)
        assert(resting.exercises.first().sets.first().setState == SetState.NotStated)
    }

    @Test
    fun `constructor rejects rest remaining unless the session is resting`() {
        val inProgress = fullBodyA.toWorkoutSession(startedAt = startedAt)
        val overview = fullBodyA.toOverviewSession()

        assertFailsWith<IllegalArgumentException> {
            OpenWorkoutSnapshot(inProgress, Duration.ZERO, restRemaining = 1.minutes)
        }
        assertFailsWith<IllegalStateException> {
            OpenWorkoutSnapshot(overview, Duration.ZERO, restRemaining = null)
        }
    }

    @Test
    fun `constructor requires rest remaining when the session is resting`() {
        val completedAt = startedAt.plusMinutes(4)
        val resting = completeAllWarmups(
            fullBodyA.toWorkoutSession(startedAt = startedAt),
            exerciseIndex = 0,
            now = completedAt,
        )

        assertFailsWith<IllegalArgumentException> {
            OpenWorkoutSnapshot(resting, 4.minutes, restRemaining = null)
        }
        val snapshot = OpenWorkoutSnapshot(resting, 4.minutes, restRemaining = 2.minutes)
        assert(!snapshot.shouldActivateNextSet)
        assert(snapshot.restRemaining == 2.minutes)
    }
}

private fun completeAllWarmups(
    session: WorkoutSession,
    exerciseIndex: Int,
    now: LocalDateTime,
): WorkoutSession {
    val warmupCount = session.exercises[exerciseIndex].warmupSets?.size ?: 0
    return (0 until warmupCount).fold(session) { current, index ->
        current.completeSet(current.exercises[exerciseIndex], index, isWarmup = true, now = now)
    }
}

private fun completeAllSets(
    session: WorkoutSession,
    exerciseIndex: Int,
    now: LocalDateTime,
): WorkoutSession {
    val withWarmups = completeAllWarmups(session, exerciseIndex, now)
    val workCount = withWarmups.exercises[exerciseIndex].sets.size
    return (0 until workCount).fold(withWarmups) { current, index ->
        current.completeSet(current.exercises[exerciseIndex], index, now = now)
    }
}
