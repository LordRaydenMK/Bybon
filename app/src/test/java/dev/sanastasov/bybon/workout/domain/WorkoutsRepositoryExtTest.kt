package dev.sanastasov.bybon.workout.domain

import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
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
}
