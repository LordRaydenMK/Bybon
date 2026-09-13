package dev.sanastasov.bybon.workout.ui.plans

import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.domain.fullBodyA
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
}
