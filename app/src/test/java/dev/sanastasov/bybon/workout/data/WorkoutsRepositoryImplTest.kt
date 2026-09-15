package dev.sanastasov.bybon.workout.data

import dev.sanastasov.bybon.workout.domain.Equipment
import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.MuscleGroup
import dev.sanastasov.bybon.workout.domain.catalogExercises
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WorkoutsRepositoryImplTest {

    @Test
    fun `exposes the built-in exercise catalog`() = runTest {
        val repository = WorkoutsRepositoryImpl()

        assert(repository.exercises().first() == catalogExercises)
    }

    @Test
    fun `importHistory appends new exercises to the catalog`() = runTest {
        val repository = WorkoutsRepositoryImpl()
        val crunch = ExerciseDefinition(
            "crunch-machine",
            "Crunch (Machine)",
            MuscleGroup.Core,
            Equipment.Machine,
        )

        repository.importHistory(
            plans = emptyList(),
            sessions = emptyList(),
            exercises = listOf(crunch),
        )

        val stored = repository.exercises().first()
        assert(stored.containsAll(catalogExercises))
        assert(stored.last() == crunch)
    }
}
