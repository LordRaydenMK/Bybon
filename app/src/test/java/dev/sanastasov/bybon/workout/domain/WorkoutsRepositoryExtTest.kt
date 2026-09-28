package dev.sanastasov.bybon.workout.domain

import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WorkoutsRepositoryExtTest {

    @Test
    fun `requireExercise returns the catalog exercise`() = runTest {
        val repository = FakeWorkoutsRepository(initialExercises = catalogExercises)

        val actual = repository.requireExercise("bench-press-bb")

        assert(actual == catalogExercise("bench-press-bb"))
    }

    @Test
    fun `requireExercise throws when the exercise is missing`() = runTest {
        val repository = FakeWorkoutsRepository(initialExercises = catalogExercises)
        val error = assertFailsWith<IllegalStateException> {
            repository.requireExercise("missing")
        }
        assert(error.message == "Exercise missing is not in the repository")
    }
}
