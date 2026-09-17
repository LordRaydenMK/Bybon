package dev.sanastasov.bybon.workout.ui.library

import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.domain.MuscleGroup
import dev.sanastasov.bybon.workout.domain.catalogExercises
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ExerciseLibraryViewModelTest {

    @Test
    fun `loads grouped exercises from the repository`() = runTest {
        val repository = FakeWorkoutsRepository(initialExercises = catalogExercises)
        val viewModel = ExerciseLibraryViewModel(repository, backgroundScope)

        val state = viewModel.uiState.first { it.groups.isNotEmpty() }

        assert(
            state.groups.map { it.bodyPart } == listOf(
                MuscleGroup.Arms,
                MuscleGroup.Back,
                MuscleGroup.Chest,
                MuscleGroup.Legs,
                MuscleGroup.Shoulders,
            ),
        )
        assert(state.groups.first().exercises.first().name == "Incline Curl (dumbbell)")
        assert(state.groups.first().exercises.first().equipmentLabel == "Dumbbell")
    }
}
