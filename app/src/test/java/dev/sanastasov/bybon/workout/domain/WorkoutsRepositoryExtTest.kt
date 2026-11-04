package dev.sanastasov.bybon.workout.domain

import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.data.completedExercise
import dev.sanastasov.bybon.workout.data.completedSession
import java.time.LocalDateTime
import kotlin.test.assertFailsWith
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WorkoutsRepositoryExtTest {

    @Test
    fun `requireExercise returns the catalog exercise`() = runTest {
        val repository = FakeWorkoutsRepository(initialExercises = catalogExercises)

        val actual = repository.requireExercise("bench-press")

        assert(actual == catalogExercise("bench-press"))
    }

    @Test
    fun `requireExercise throws when the exercise is missing`() = runTest {
        val repository = FakeWorkoutsRepository(initialExercises = catalogExercises)
        val error = assertFailsWith<IllegalStateException> {
            repository.requireExercise("missing")
        }
        assert(error.message == "Exercise missing is not in the repository")
    }

    @Test
    fun `updateWorkout waits for an in-progress session when the first snapshot is empty`() =
        runTest {
            val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
            val started = fullBodyA.toWorkoutSession()
            val update = async {
                repository.updateWorkout(fullBodyA.id) { it.copy(note = "edited") }
            }

            repository.emitSessions(listOf(started))

            assert(update.await().note == "edited")
            assert(repository.workoutSessions().first().single().note == "edited")
        }

    @Test
    fun `completeInProgressSet marks the in-progress set complete`() = runTest {
        val started = fullBodyA.toWorkoutSession()
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialSessions = listOf(started),
        )

        val updated = checkNotNull(repository.completeInProgressSet())

        val warmup = checkNotNull(updated.exercises.first().warmupSets)
        assert(warmup.first().setState is SetState.Completed)
        assert(warmup[1].setState == SetState.InProgress)
        assert(repository.workoutSessions().first().single() == updated)
    }

    @Test
    fun `completeInProgressSet is null when there is no in-progress session`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))

        assert(repository.completeInProgressSet() == null)
    }

    @Test
    fun `notificationSummarySessionId is the latest completed session when none are live`() =
        runTest {
            val older = completedSession(
                planId = fullBodyA.id.id,
                planName = fullBodyA.name,
                startedAt = LocalDateTime.of(2026, 1, 1, 12, 0),
                exercises = listOf(completedExercise("bench-press", 50f to 8)),
            )
            val newer = completedSession(
                planId = fullBodyA.id.id,
                planName = fullBodyA.name,
                startedAt = LocalDateTime.of(2026, 2, 1, 12, 0),
                exercises = listOf(completedExercise("bench-press", 50f to 8)),
            )
            val repository = FakeWorkoutsRepository(
                initialPlans = listOf(fullBodyA),
                initialSessions = listOf(older, newer),
            )

            assert(repository.notificationSummarySessionId(fullBodyA.id) == newer.id)
        }

    @Test
    fun `notificationSummarySessionId is null while a session is in progress`() = runTest {
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialSessions = listOf(fullBodyA.toWorkoutSession()),
        )

        assert(repository.notificationSummarySessionId(fullBodyA.id) == null)
    }
}
