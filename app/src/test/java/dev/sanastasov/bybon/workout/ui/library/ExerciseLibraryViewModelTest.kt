package dev.sanastasov.bybon.workout.ui.library

import app.cash.turbine.test
import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.domain.MuscleGroup
import dev.sanastasov.bybon.workout.domain.catalogExercise
import dev.sanastasov.bybon.workout.domain.catalogExercises
import dev.sanastasov.bybon.workout.domain.fullBodyA
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ExerciseLibraryViewModelTest {

    @Test
    fun `loads grouped exercises from the repository`() = runTest {
        val viewModel = viewModel()

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
        assert(!state.addEnabled)
        assert(state.groups.flatMap { it.exercises }.none { it.selected })
    }

    @Test
    fun `toggling an exercise selects it and enables add`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { it.groups.isNotEmpty() }

        viewModel.onAction(ExerciseLibraryAction.OnToggleExercise("incline-curl-db"))

        val state = viewModel.uiState.first { it.addEnabled }
        assert(state.exercise("incline-curl-db").selected)
        assert(state.groups.flatMap { it.exercises }.single { it.selected }.id == "incline-curl-db")
    }

    @Test
    fun `toggling the selected exercise clears the selection`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { it.groups.isNotEmpty() }
        viewModel.onAction(ExerciseLibraryAction.OnToggleExercise("incline-curl-db"))
        viewModel.uiState.first { it.addEnabled }

        viewModel.onAction(ExerciseLibraryAction.OnToggleExercise("incline-curl-db"))

        val state = viewModel.uiState.first { !it.addEnabled && it.groups.isNotEmpty() }
        assert(!state.exercise("incline-curl-db").selected)
    }

    @Test
    fun `toggling another exercise moves the selection`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { it.groups.isNotEmpty() }
        viewModel.onAction(ExerciseLibraryAction.OnToggleExercise("incline-curl-db"))
        viewModel.uiState.first { it.addEnabled }

        viewModel.onAction(ExerciseLibraryAction.OnToggleExercise("bench-press-bb"))

        val state = viewModel.uiState.first { it.exercise("bench-press-bb").selected }
        assert(state.addEnabled)
        assert(!state.exercise("incline-curl-db").selected)
    }

    @Test
    fun `adding the selected exercise persists it and navigates back`() = runTest {
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialExercises = catalogExercises,
        )
        val viewModel = ExerciseLibraryViewModel(fullBodyA.id, repository, backgroundScope)
        viewModel.uiState.first { it.groups.isNotEmpty() }
        viewModel.onAction(ExerciseLibraryAction.OnToggleExercise("incline-curl-db"))
        viewModel.uiState.first { it.addEnabled }

        viewModel.onAction(ExerciseLibraryAction.OnAddExercise)

        assert(viewModel.effects.first() == ExerciseLibraryEffect.NavigateBack)
        val added = repository.workoutPlans().first().single().sets.last()
        assert(added.exercise == catalogExercise("incline-curl-db"))
        assert(added.sets == 3)
        assert(added.warmupSets == 0)
        assert(added.repRange == 8..12)
        assert(added.restAfterWorkSet == 1.minutes)
        assert(repository.workoutPlans().first().single().sets.dropLast(1) == fullBodyA.sets)
    }

    @Test
    fun `adding with no selection does not change the plan`() = runTest {
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialExercises = catalogExercises,
        )
        val viewModel = ExerciseLibraryViewModel(fullBodyA.id, repository, backgroundScope)
        viewModel.uiState.first { it.groups.isNotEmpty() }

        viewModel.effects.test {
            viewModel.onAction(ExerciseLibraryAction.OnAddExercise)
            advanceUntilIdle()
            expectNoEvents()
        }
        assert(repository.workoutPlans().first() == listOf(fullBodyA))
    }

    @Test
    fun `adding an exercise already on the plan navigates back without duplicating`() = runTest {
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialExercises = catalogExercises,
        )
        val viewModel = ExerciseLibraryViewModel(fullBodyA.id, repository, backgroundScope)
        viewModel.uiState.first { it.groups.isNotEmpty() }
        viewModel.onAction(ExerciseLibraryAction.OnToggleExercise("bench-press-bb"))
        viewModel.uiState.first { it.addEnabled }

        viewModel.onAction(ExerciseLibraryAction.OnAddExercise)

        assert(viewModel.effects.first() == ExerciseLibraryEffect.NavigateBack)
        assert(repository.workoutPlans().first() == listOf(fullBodyA))
    }

    private fun viewModel(
        repository: FakeWorkoutsRepository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialExercises = catalogExercises,
        ),
    ) = ExerciseLibraryViewModel(fullBodyA.id, repository, backgroundScope)

    private fun ExerciseLibraryUiState.exercise(id: String) =
        groups.flatMap { it.exercises }.first { it.id == id }
}
