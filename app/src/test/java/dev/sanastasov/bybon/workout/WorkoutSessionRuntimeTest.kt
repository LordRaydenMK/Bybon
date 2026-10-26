package dev.sanastasov.bybon.workout

import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.completeSet
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WorkoutSessionRuntimeTest {

    private val startedAt = LocalDateTime.of(2026, 10, 24, 12, 0, 0)

    @Test
    fun `null means no live workout`() = runTest {
        val runtime = WorkoutSessionRuntimeImpl(
            FakeWorkoutsRepository(initialPlans = listOf(fullBodyA)),
            backgroundScope,
            now = { startedAt },
        )

        runCurrent()
        assert(runtime.state.value == null)
    }

    @Test
    fun `in progress snapshot maps elapsed from the hosted clock`() = runTest {
        val session = fullBodyA.toWorkoutSession(startedAt = startedAt)
        val runtime = WorkoutSessionRuntimeImpl(
            FakeWorkoutsRepository(
                initialPlans = listOf(fullBodyA),
                initialSessions = listOf(session),
            ),
            backgroundScope,
            now = { startedAt.plusNanos(testScheduler.currentTime * 1_000_000) },
        )

        runCurrent()
        assert(runtime.state.value != null)
        assert(runtime.state.value!!.elapsed == Duration.ZERO)
        assert(runtime.state.value!!.restRemaining == null)
        assert(runtime.state.value!!.session.id == session.id)

        advanceTimeBy(5.seconds)
        runCurrent()
        assert(runtime.state.value!!.elapsed == 5.seconds)
        assert(runtime.state.value!!.session.state is WorkoutState.InProgress)
    }

    @Test
    fun `expired rest persists startWorkout`() = runTest {
        val completedAt = startedAt.plusMinutes(4)
        val resting = completeAllWarmups(
            fullBodyA.toWorkoutSession(startedAt = startedAt),
            now = completedAt,
        )
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialSessions = listOf(resting),
        )
        WorkoutSessionRuntimeImpl(
            repository,
            backgroundScope,
            now = { completedAt.plusNanos(testScheduler.currentTime * 1_000_000) },
        )

        runCurrent()
        assert(repository.workoutSessions().first().single().state is WorkoutState.Resting)

        advanceTimeBy(2.minutes)
        runCurrent()
        val activated = repository.workoutSessions().first().single()
        assert(activated.state is WorkoutState.InProgress)
        assert(activated.exercises.first().sets.first().setState == SetState.InProgress)
    }

    @Test
    fun `rest remaining above zero stays resting`() = runTest {
        val completedAt = startedAt.plusMinutes(4)
        val resting = completeAllWarmups(
            fullBodyA.toWorkoutSession(startedAt = startedAt),
            now = completedAt,
        )
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialSessions = listOf(resting),
        )
        val runtime = WorkoutSessionRuntimeImpl(
            repository,
            backgroundScope,
            now = { completedAt.plusNanos(testScheduler.currentTime * 1_000_000) },
        )

        runCurrent()
        assert(runtime.state.value!!.restRemaining == 2.minutes)
        assert(!runtime.state.value!!.shouldActivateNextSet)

        advanceTimeBy(30.seconds)
        runCurrent()
        assert(runtime.state.value!!.restRemaining == 2.minutes - 30.seconds)
        assert(runtime.state.value!!.session.state is WorkoutState.Resting)
        assert(
            repository.workoutSessions().first().single()
                .exercises.first().sets.first().setState == SetState.NotStated,
        )
    }
}

private fun completeAllWarmups(session: WorkoutSession, now: LocalDateTime): WorkoutSession {
    val warmupCount = session.exercises.first().warmupSets?.size ?: 0
    return (0 until warmupCount).fold(session) { current, index ->
        current.completeSet(current.exercises.first(), index, isWarmup = true, now = now)
    }
}
