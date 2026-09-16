package dev.sanastasov.bybon.workout.ui.plans

import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class EditPlanViewModelTest {

    @Test
    fun `loads the matching plan`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA, fullBodyB))
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)

        val state = viewModel.uiState.first { it != null }
        assert(state == fullBodyA)
    }

    @Test
    fun `archiving the plan hides it and navigates back`() = runTest {
        val repository: WorkoutsRepository =
            FakeWorkoutsRepository(initialPlans = listOf(fullBodyA, fullBodyB))
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.first { it != null }
        viewModel.onAction(EditPlanAction.OnArchivePlan)

        assert(viewModel.effects.first() == EditPlanEffect.NavigateBack)
        assert(repository.workoutPlans().first() == listOf(fullBodyB))
    }

    @Test
    fun `adding a set persists a work set on the planned exercise`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA, fullBodyB))
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.first { it != null }
        viewModel.onAction(EditPlanAction.OnAddSet("bench-press-bb"))

        val updated = viewModel.uiState.first { it?.sets?.first()?.sets == 4 }!!
        assert(updated.sets.first().warmupSets == 3)
        assert(repository.workoutPlans().first().first().sets.first().sets == 4)
        assert(repository.workoutPlans().first()[1] == fullBodyB)
    }

    @Test
    fun `removing the last set persists one fewer work set`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.first { it != null }
        viewModel.onAction(EditPlanAction.OnRemoveLastSet("leg-curl"))

        val updated = viewModel.uiState.first { plan ->
            plan?.sets?.first { it.exercise.id == "leg-curl" }?.sets == 2
        }!!
        assert(updated.sets.first { it.exercise.id == "leg-curl" }.sets == 2)
        assert(updated.sets.first().sets == 3)
    }

    @Test
    fun `removing an exercise persists the plan without it`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.first { it != null }
        viewModel.onAction(EditPlanAction.OnRemoveExercise("leg-curl"))

        val updated = viewModel.uiState.first { plan ->
            plan?.sets?.none { it.exercise.id == "leg-curl" } == true
        }!!
        assert(updated.sets.none { it.exercise.id == "leg-curl" })
        assert(updated.sets.size == fullBodyA.sets.size - 1)
    }
}
