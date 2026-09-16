package dev.sanastasov.bybon.workout.ui.plans

import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.domain.WorkoutPlansFilter
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
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA, fullBodyB))
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.first { it != null }
        viewModel.onAction(EditPlanAction.OnArchivePlan)

        assert(viewModel.effects.first() == EditPlanEffect.NavigateBack)
        assert(repository.workoutPlans(WorkoutPlansFilter.ActivePlans).first() == listOf(fullBodyB))
    }
}
