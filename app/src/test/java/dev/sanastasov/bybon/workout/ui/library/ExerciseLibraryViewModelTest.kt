package dev.sanastasov.bybon.workout.ui.library

import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.domain.BodyPart
import dev.sanastasov.bybon.workout.domain.Equipment
import dev.sanastasov.bybon.workout.domain.catalogExercises
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.label
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ExerciseLibraryViewModelTest {

    @Test
    fun `loads grouped exercises from the repository`() = runTest {
        val viewModel = viewModel()

        val state = viewModel.uiState.first { it.groups.isNotEmpty() }

        val stockedGroups = BodyPart.entries.filter { group ->
            catalogExercises.any { it.bodyPart == group }
        }
        val firstExercise = catalogExercises.first { it.bodyPart == stockedGroups.first() }
        assert(state.groups.map { it.bodyPart } == stockedGroups)
        assert(state.groups.first().exercises.first().name == firstExercise.name)
        assert(state.groups.first().exercises.first().equipment == firstExercise.equipment)
        assert(
            state.groups.first().exercises.first().equipmentLabel == firstExercise.equipment?.label,
        )
        assert(!state.addEnabled)
        assert(state.groups.flatMap { it.exercises }.none { it.selected })
        assert(state.filterChips.none { it.selected })
        assert(state.filterChips.none { it.id == ExerciseLibraryFilterId.ClearAll })
        assert(!state.showEmptyState)
        assert(state.existingHeader == "Already added")
        assert(
            state.existingExercises.map { it.id } == fullBodyA.sets.map { it.exercise.id },
        )
        assert(state.existingExercises.none { it.selectable })
        assert(
            state.groups.flatMap { it.exercises }.none { exercise ->
                exercise.id in fullBodyA.sets.map { it.exercise.id }
            },
        )
    }

    @Test
    fun `toggling an exercise selects it and enables add`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { it.groups.isNotEmpty() }

        viewModel.onAction(ExerciseLibraryAction.OnToggleExercise("incline-db-curl"))

        val state = viewModel.uiState.first { it.addEnabled }
        assert(state.exercise("incline-db-curl").selected)
        assert(state.exercise("incline-db-curl").selectable)
        assert(state.selectedExerciseId == "incline-db-curl")
        assert(state.existingExercises.last().id == "incline-db-curl")
        assert(state.groups.flatMap { it.exercises }.none { it.id == "incline-db-curl" })
    }

    @Test
    fun `toggling the selected exercise clears the selection`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { it.groups.isNotEmpty() }
        viewModel.onAction(ExerciseLibraryAction.OnToggleExercise("incline-db-curl"))
        viewModel.uiState.first { it.addEnabled }

        viewModel.onAction(ExerciseLibraryAction.OnToggleExercise("incline-db-curl"))

        val state = viewModel.uiState.first { !it.addEnabled && it.groups.isNotEmpty() }
        assert(!state.exercise("incline-db-curl").selected)
        assert(state.groups.flatMap { it.exercises }.any { it.id == "incline-db-curl" })
        assert(state.existingExercises.none { it.id == "incline-db-curl" })
    }

    @Test
    fun `toggling another exercise moves the selection`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { it.groups.isNotEmpty() }
        viewModel.onAction(ExerciseLibraryAction.OnToggleExercise("incline-db-curl"))
        viewModel.uiState.first { it.addEnabled }

        viewModel.onAction(ExerciseLibraryAction.OnToggleExercise("incline-db-press"))

        val state = viewModel.uiState.first { it.exercise("incline-db-press").selected }
        assert(state.addEnabled)
        assert(!state.exercise("incline-db-curl").selected)
        assert(state.existingExercises.last().id == "incline-db-press")
        assert(state.groups.flatMap { it.exercises }.any { it.id == "incline-db-curl" })
        assert(state.groups.flatMap { it.exercises }.none { it.id == "incline-db-press" })
    }

    @Test
    fun `adding the selected exercise emits the picked id`() = runTest {
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialExercises = catalogExercises,
        )
        val viewModel = ExerciseLibraryViewModel(
            fullBodyA.sets.map { it.exercise.id },
            repository,
            backgroundScope,
        )
        viewModel.uiState.first { it.groups.isNotEmpty() }
        viewModel.onAction(ExerciseLibraryAction.OnToggleExercise("incline-db-curl"))
        viewModel.uiState.first { it.addEnabled }

        viewModel.onAction(ExerciseLibraryAction.OnAddExercise("incline-db-curl"))

        assert(
            viewModel.effects.first() ==
                ExerciseLibraryEffect.ExercisePicked("incline-db-curl"),
        )
        assert(repository.workoutPlans().first() == listOf(fullBodyA))
    }

    @Test
    fun `adding an already added exercise is rejected`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { it.groups.isNotEmpty() }
        val error = assertFailsWith<IllegalStateException> {
            viewModel.onAction(ExerciseLibraryAction.OnAddExercise("bench-press"))
        }
        assert(error.message == "Exercise bench-press is already added")
    }

    @Test
    fun `toggling an existing exercise does not select it`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { it.groups.isNotEmpty() }

        viewModel.onAction(ExerciseLibraryAction.OnToggleExercise("bench-press"))

        val state = viewModel.uiState.first { it.existingExercises.isNotEmpty() }
        assert(!state.addEnabled)
        assert(state.selectedExerciseId == null)
        assert(state.existingExercises.none { it.selected })
        assert(state.existingExercises.any { it.id == "bench-press" && !it.selectable })
        assert(state.groups.flatMap { it.exercises }.none { it.id == "bench-press" })
    }

    @Test
    fun `toggling a muscle group chip keeps matching groups`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { it.groups.isNotEmpty() }

        viewModel.onAction(
            ExerciseLibraryAction.OnToggleFilter(
                ExerciseLibraryFilterId.BodyPartFilter(BodyPart.UpperArms),
            ),
        )

        val state = viewModel.uiState.first { it.groups.size == 1 }
        assert(state.groups.single().bodyPart == BodyPart.UpperArms)
        val armsChip = state.filterChips.single {
            it.id == ExerciseLibraryFilterId.BodyPartFilter(BodyPart.UpperArms)
        }
        assert(armsChip.selected)
        assert(state.filterChips.first().id == ExerciseLibraryFilterId.ClearAll)
        assert(!state.showEmptyState)
    }

    @Test
    fun `muscle group chips of the same category are OR`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { it.groups.isNotEmpty() }

        viewModel.onAction(
            ExerciseLibraryAction.OnToggleFilter(
                ExerciseLibraryFilterId.BodyPartFilter(BodyPart.UpperArms),
            ),
        )
        viewModel.onAction(
            ExerciseLibraryAction.OnToggleFilter(
                ExerciseLibraryFilterId.BodyPartFilter(BodyPart.UpperLegs),
            ),
        )

        val expectedGroups = listOf(BodyPart.UpperLegs, BodyPart.UpperArms)
        val state = viewModel.uiState.first { state ->
            state.groups.map { it.bodyPart } == expectedGroups
        }
        assert(state.groups.flatMap { it.exercises }.any { it.id == "incline-db-curl" })
        assert(state.groups.flatMap { it.exercises }.any { it.id == "leg-press" })
        assert(state.groups.flatMap { it.exercises }.none { it.id == "bench-press" })
        assert(state.groups.flatMap { it.exercises }.none { it.id == "squat" })
    }

    @Test
    fun `muscle group and equipment chips are AND`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { it.groups.isNotEmpty() }

        viewModel.onAction(
            ExerciseLibraryAction.OnToggleFilter(
                ExerciseLibraryFilterId.BodyPartFilter(BodyPart.UpperArms),
            ),
        )
        viewModel.onAction(
            ExerciseLibraryAction.OnToggleFilter(
                ExerciseLibraryFilterId.EquipmentFilter(Equipment.Dumbbell),
            ),
        )

        val state = viewModel.uiState.first { current ->
            val exercises = current.groups.singleOrNull()?.exercises.orEmpty()
            exercises.isNotEmpty() &&
                exercises.all { it.equipment == Equipment.Dumbbell } &&
                exercises.any { it.id == "incline-db-curl" }
        }
        assert(state.groups.single().bodyPart == BodyPart.UpperArms)
        assert(state.groups.single().exercises.none { it.id == "machine-bicep-curl" })
        assert(state.existingExercises.any { it.id == "db-skull-crusher" && !it.selectable })
    }

    @Test
    fun `toggling a selected chip removes that filter`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { it.groups.isNotEmpty() }
        viewModel.onAction(
            ExerciseLibraryAction.OnToggleFilter(
                ExerciseLibraryFilterId.BodyPartFilter(BodyPart.UpperArms),
            ),
        )
        viewModel.uiState.first { it.groups.size == 1 }

        viewModel.onAction(
            ExerciseLibraryAction.OnToggleFilter(
                ExerciseLibraryFilterId.BodyPartFilter(BodyPart.UpperArms),
            ),
        )

        val state = viewModel.uiState.first { it.groups.size > 1 }
        assert(state.filterChips.none { it.selected })
        assert(state.filterChips.none { it.id == ExerciseLibraryFilterId.ClearAll })
    }

    @Test
    fun `clear all removes every selected filter`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { it.groups.isNotEmpty() }
        viewModel.onAction(
            ExerciseLibraryAction.OnToggleFilter(
                ExerciseLibraryFilterId.BodyPartFilter(BodyPart.UpperArms),
            ),
        )
        viewModel.onAction(
            ExerciseLibraryAction.OnToggleFilter(
                ExerciseLibraryFilterId.EquipmentFilter(Equipment.Dumbbell),
            ),
        )
        viewModel.uiState.first { state ->
            state.filterChips.any { it.id == ExerciseLibraryFilterId.ClearAll }
        }

        viewModel.onAction(ExerciseLibraryAction.OnToggleFilter(ExerciseLibraryFilterId.ClearAll))

        val state = viewModel.uiState.first { current ->
            current.filterChips.none { it.selected }
        }
        assert(state.filterChips.none { it.id == ExerciseLibraryFilterId.ClearAll })
        val stockedGroups = BodyPart.entries.filter { group ->
            catalogExercises.any { it.bodyPart == group }
        }
        assert(state.groups.map { it.bodyPart } == stockedGroups)
    }

    @Test
    fun `filters with no matches show the empty state`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { it.groups.isNotEmpty() }

        viewModel.onAction(
            ExerciseLibraryAction.OnToggleFilter(
                ExerciseLibraryFilterId.BodyPartFilter(BodyPart.LowerArms),
            ),
        )
        viewModel.onAction(
            ExerciseLibraryAction.OnToggleFilter(
                ExerciseLibraryFilterId.EquipmentFilter(Equipment.AssistedBodyWeight),
            ),
        )

        val state = viewModel.uiState.first { it.showEmptyState }
        assert(state.groups.isEmpty())
        assert(state.existingHeader == "Already added")
        assert(state.existingExercises.isNotEmpty())
        val lowerArms = state.filterChips.single {
            it.id == ExerciseLibraryFilterId.BodyPartFilter(BodyPart.LowerArms)
        }
        assert(lowerArms.selected)
    }

    @Test
    fun `selected exercise stays enabled after it is filtered out`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { it.groups.isNotEmpty() }
        viewModel.onAction(ExerciseLibraryAction.OnToggleExercise("incline-db-curl"))
        viewModel.uiState.first { it.addEnabled }

        viewModel.onAction(
            ExerciseLibraryAction.OnToggleFilter(
                ExerciseLibraryFilterId.BodyPartFilter(BodyPart.Chest),
            ),
        )

        val state = viewModel.uiState.first {
            it.groups.singleOrNull()?.bodyPart == BodyPart.Chest
        }
        assert(state.selectedExerciseId == "incline-db-curl")
        assert(state.addEnabled)
        assert(state.existingExercises.last().id == "incline-db-curl")
        assert(state.existingExercises.last().selected)
        assert(state.groups.flatMap { it.exercises }.none { it.id == "incline-db-curl" })
    }

    private fun TestScope.viewModel(
        repository: FakeWorkoutsRepository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialExercises = catalogExercises,
        ),
    ) = ExerciseLibraryViewModel(
        fullBodyA.sets.map { it.exercise.id },
        repository,
        backgroundScope,
    )

    private fun ExerciseLibraryUiState.exercise(id: String) =
        (existingExercises + groups.flatMap { it.exercises }).first { it.id == id }
}
