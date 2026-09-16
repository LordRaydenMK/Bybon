package dev.sanastasov.bybon.workout.ui.plans

import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WorkoutPlansViewModelTest {

    @Test
    fun `starting an inactive plan opens the overview`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutPlansViewModel(repository, backgroundScope)

        viewModel.uiState.first { it.isNotEmpty() }
        viewModel.onAction(WorkoutPlansAction.OnStartPlan(fullBodyA))
        assert(viewModel.effects.first() == WorkoutPlanEffect.OpenOverview(fullBodyA))
    }

    @Test
    fun `starting an active plan opens the session`() = runTest {
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialSessions = listOf(fullBodyA.toWorkoutSession()),
        )
        val viewModel = WorkoutPlansViewModel(repository, backgroundScope)

        viewModel.uiState.first { plans -> plans.any { it.isActive } }
        viewModel.onAction(WorkoutPlansAction.OnStartPlan(fullBodyA))
        assert(viewModel.effects.first() == WorkoutPlanEffect.OpenSession(fullBodyA))
    }

    @Test
    fun `editing a plan opens the edit screen`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutPlansViewModel(repository, backgroundScope)

        viewModel.uiState.first { it.isNotEmpty() }
        viewModel.onAction(WorkoutPlansAction.OnEditPlan(fullBodyA))
        assert(viewModel.effects.first() == WorkoutPlanEffect.OpenEditPlan(fullBodyA))
    }

    @Test
    fun `archived plans are omitted from the list`() = runTest {
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA, fullBodyB),
            initialArchivedPlanIds = setOf(fullBodyA.id),
        )
        val viewModel = WorkoutPlansViewModel(repository, backgroundScope)

        val plans = viewModel.uiState.first { it.isNotEmpty() }
        assert(plans.map { it.plan } == listOf(fullBodyB))
    }

    @Test
    fun `archiving a plan removes it from the list`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA, fullBodyB))
        val viewModel = WorkoutPlansViewModel(repository, backgroundScope)

        viewModel.uiState.first { it.size == 2 }
        viewModel.onAction(WorkoutPlansAction.OnArchivePlan(fullBodyA))
        val plans = viewModel.uiState.first { it.size == 1 }
        assert(plans.single().plan == fullBodyB)
    }
}
