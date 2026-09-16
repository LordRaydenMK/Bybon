package dev.sanastasov.bybon.workout.ui.plans

import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
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
}
