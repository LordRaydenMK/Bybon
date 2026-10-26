package dev.sanastasov.bybon.workout

import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import java.time.LocalDateTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WorkoutForegroundDriverTest {

    private val startedAt = LocalDateTime.of(2026, 10, 24, 12, 0, 0)

    @Test
    fun `starts when an open snapshot appears and stops when it is gone`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        var started = 0
        var stopped = 0
        WorkoutForegroundDriver(
            repository = repository,
            scope = backgroundScope,
            now = { startedAt },
            startService = { started++ },
            stopService = { stopped++ },
        ).start()

        runCurrent()
        assert(started == 0)
        assert(stopped == 0)

        val session = fullBodyA.toWorkoutSession(startedAt = startedAt)
        repository.updateWorkout(session)
        runCurrent()
        assert(started == 1)
        assert(stopped == 0)

        repository.deleteWorkout(session.id)
        runCurrent()
        assert(started == 1)
        assert(stopped == 1)
    }
}
