package dev.sanastasov.bybon.workout.ui.library

import app.cash.turbine.test
import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.domain.Equipment
import dev.sanastasov.bybon.workout.domain.MuscleGroup
import dev.sanastasov.bybon.workout.domain.catalogExercise
import dev.sanastasov.bybon.workout.domain.catalogExercises
import dev.sanastasov.bybon.workout.domain.fullBodyA
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
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
        assert(state.groups.first().exercises.first().equipment == Equipment.Dumbbell)
        assert(state.groups.first().exercises.first().equipmentLabel == "Dumbbell")
        assert(!state.addEnabled)
        assert(state.groups.flatMap { it.exercises }.none { it.selected })
        assert(state.filterChips.none { it.selected })
        assert(state.filterChips.none { it.id == ExerciseLibraryFilterId.ClearAll })
        assert(!state.showEmptyState)
        assert(state.planName == "Full Body A")
        assert(state.inPlanHeader == "In plan Full Body A")
        assert(
            state.inPlanExercises.map { it.id } == fullBodyA.sets.map { it.exercise.id },
        )
        assert(state.inPlanExercises.none { it.selectable })
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

        viewModel.onAction(ExerciseLibraryAction.OnToggleExercise("incline-curl-db"))

        val state = viewModel.uiState.first { it.addEnabled }
        assert(state.exercise("incline-curl-db").selected)
        assert(state.exercise("incline-curl-db").selectable)
        assert(state.selectedExerciseId == "incline-curl-db")
        assert(state.inPlanExercises.last().id == "incline-curl-db")
        assert(state.groups.flatMap { it.exercises }.none { it.id == "incline-curl-db" })
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
        assert(state.groups.flatMap { it.exercises }.any { it.id == "incline-curl-db" })
        assert(state.inPlanExercises.none { it.id == "incline-curl-db" })
    }

    @Test
    fun `toggling another exercise moves the selection`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { it.groups.isNotEmpty() }
        viewModel.onAction(ExerciseLibraryAction.OnToggleExercise("incline-curl-db"))
        viewModel.uiState.first { it.addEnabled }

        viewModel.onAction(ExerciseLibraryAction.OnToggleExercise("incline-bench-press-db"))

        val state = viewModel.uiState.first { it.exercise("incline-bench-press-db").selected }
        assert(state.addEnabled)
        assert(!state.exercise("incline-curl-db").selected)
        assert(state.inPlanExercises.last().id == "incline-bench-press-db")
        assert(state.groups.flatMap { it.exercises }.any { it.id == "incline-curl-db" })
        assert(state.groups.flatMap { it.exercises }.none { it.id == "incline-bench-press-db" })
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

        viewModel.onAction(ExerciseLibraryAction.OnAddExercise("incline-curl-db"))

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
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `adding an unknown exercise does not change the plan`() = runTest {
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialExercises = catalogExercises,
        )
        val viewModel = ExerciseLibraryViewModel(fullBodyA.id, repository, backgroundScope)
        viewModel.uiState.first { it.groups.isNotEmpty() }

        viewModel.effects.test {
            viewModel.onAction(ExerciseLibraryAction.OnAddExercise("missing"))
            advanceUntilIdle()
            expectNoEvents()
        }
        assert(repository.workoutPlans().first() == listOf(fullBodyA))
    }

    @Test
    fun `toggling an exercise already on the plan does not select it`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { it.groups.isNotEmpty() }

        viewModel.onAction(ExerciseLibraryAction.OnToggleExercise("bench-press-bb"))

        val state = viewModel.uiState.first { it.inPlanExercises.isNotEmpty() }
        assert(!state.addEnabled)
        assert(state.selectedExerciseId == null)
        assert(state.inPlanExercises.none { it.selected })
        assert(state.inPlanExercises.any { it.id == "bench-press-bb" && !it.selectable })
        assert(state.groups.flatMap { it.exercises }.none { it.id == "bench-press-bb" })
    }

    @Test
    fun `toggling a muscle group chip keeps matching groups`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { it.groups.isNotEmpty() }

        viewModel.onAction(
            ExerciseLibraryAction.OnToggleFilter(
                ExerciseLibraryFilterId.MuscleGroupFilter(MuscleGroup.Arms),
            ),
        )

        val state = viewModel.uiState.first { it.groups.size == 1 }
        assert(state.groups.single().bodyPart == MuscleGroup.Arms)
        val armsChip = state.filterChips.single {
            it.id == ExerciseLibraryFilterId.MuscleGroupFilter(MuscleGroup.Arms)
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
                ExerciseLibraryFilterId.MuscleGroupFilter(MuscleGroup.Arms),
            ),
        )
        viewModel.onAction(
            ExerciseLibraryAction.OnToggleFilter(
                ExerciseLibraryFilterId.MuscleGroupFilter(MuscleGroup.Legs),
            ),
        )

        val expectedGroups = listOf(MuscleGroup.Arms, MuscleGroup.Legs)
        val state = viewModel.uiState.first { state ->
            state.groups.map { it.bodyPart } == expectedGroups
        }
        assert(state.groups.flatMap { it.exercises }.any { it.id == "incline-curl-db" })
        assert(state.groups.flatMap { it.exercises }.any { it.id == "leg-press" })
        assert(state.groups.flatMap { it.exercises }.none { it.id == "bench-press-bb" })
        assert(state.groups.flatMap { it.exercises }.none { it.id == "squat-bb" })
    }

    @Test
    fun `muscle group and equipment chips are AND`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { it.groups.isNotEmpty() }

        viewModel.onAction(
            ExerciseLibraryAction.OnToggleFilter(
                ExerciseLibraryFilterId.MuscleGroupFilter(MuscleGroup.Arms),
            ),
        )
        viewModel.onAction(
            ExerciseLibraryAction.OnToggleFilter(
                ExerciseLibraryFilterId.EquipmentFilter(Equipment.Dumbbell),
            ),
        )

        val state = viewModel.uiState.first {
            it.groups.singleOrNull()?.exercises?.map { exercise -> exercise.id } ==
                listOf("incline-curl-db")
        }
        assert(state.groups.single().bodyPart == MuscleGroup.Arms)
        assert(state.groups.single().exercises.none { it.id == "biceps-curl-machine" })
        assert(state.inPlanExercises.any { it.id == "skullcrusher-db" && !it.selectable })
    }

    @Test
    fun `toggling a selected chip removes that filter`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { it.groups.isNotEmpty() }
        viewModel.onAction(
            ExerciseLibraryAction.OnToggleFilter(
                ExerciseLibraryFilterId.MuscleGroupFilter(MuscleGroup.Arms),
            ),
        )
        viewModel.uiState.first { it.groups.size == 1 }

        viewModel.onAction(
            ExerciseLibraryAction.OnToggleFilter(
                ExerciseLibraryFilterId.MuscleGroupFilter(MuscleGroup.Arms),
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
                ExerciseLibraryFilterId.MuscleGroupFilter(MuscleGroup.Arms),
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
        assert(
            state.groups.map { it.bodyPart } == listOf(
                MuscleGroup.Arms,
                MuscleGroup.Back,
                MuscleGroup.Chest,
                MuscleGroup.Legs,
                MuscleGroup.Shoulders,
            ),
        )
    }

    @Test
    fun `filters with no matches show the empty state`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { it.groups.isNotEmpty() }

        viewModel.onAction(
            ExerciseLibraryAction.OnToggleFilter(
                ExerciseLibraryFilterId.MuscleGroupFilter(MuscleGroup.Core),
            ),
        )

        val state = viewModel.uiState.first { it.showEmptyState }
        assert(state.groups.isEmpty())
        assert(state.inPlanHeader == "In plan Full Body A")
        assert(state.inPlanExercises.isNotEmpty())
        val coreChip = state.filterChips.single {
            it.id == ExerciseLibraryFilterId.MuscleGroupFilter(MuscleGroup.Core)
        }
        assert(coreChip.selected)
    }

    @Test
    fun `selected exercise stays enabled after it is filtered out`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { it.groups.isNotEmpty() }
        viewModel.onAction(ExerciseLibraryAction.OnToggleExercise("incline-curl-db"))
        viewModel.uiState.first { it.addEnabled }

        viewModel.onAction(
            ExerciseLibraryAction.OnToggleFilter(
                ExerciseLibraryFilterId.MuscleGroupFilter(MuscleGroup.Chest),
            ),
        )

        val state = viewModel.uiState.first {
            it.groups.singleOrNull()?.bodyPart == MuscleGroup.Chest
        }
        assert(state.selectedExerciseId == "incline-curl-db")
        assert(state.addEnabled)
        assert(state.inPlanExercises.last().id == "incline-curl-db")
        assert(state.inPlanExercises.last().selected)
        assert(state.groups.flatMap { it.exercises }.none { it.id == "incline-curl-db" })
    }

    private fun TestScope.viewModel(
        repository: FakeWorkoutsRepository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialExercises = catalogExercises,
        ),
    ) = ExerciseLibraryViewModel(fullBodyA.id, repository, backgroundScope)

    private fun ExerciseLibraryUiState.exercise(id: String) =
        (inPlanExercises + groups.flatMap { it.exercises }).first { it.id == id }
}
