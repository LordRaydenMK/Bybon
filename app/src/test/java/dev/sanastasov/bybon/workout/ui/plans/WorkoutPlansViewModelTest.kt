package dev.sanastasov.bybon.workout.ui.plans

import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import dev.sanastasov.bybon.workout.domain.upperBodyA
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WorkoutPlansViewModelTest {

    @Test
    fun `starting an inactive plan opens the overview`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutPlansViewModel(repository, backgroundScope)

        viewModel.uiState.first { it.activePlans.isNotEmpty() }
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

        viewModel.uiState.first { state -> state.activePlans.any { it.isActive } }
        viewModel.onAction(WorkoutPlansAction.OnStartPlan(fullBodyA))
        assert(viewModel.effects.first() == WorkoutPlanEffect.OpenSession(fullBodyA))
    }

    @Test
    fun `editing a plan opens the edit screen`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutPlansViewModel(repository, backgroundScope)

        viewModel.uiState.first { it.activePlans.isNotEmpty() }
        viewModel.onAction(WorkoutPlansAction.OnEditPlan(fullBodyA))
        assert(viewModel.effects.first() == WorkoutPlanEffect.OpenEditPlan(fullBodyA))
    }

    @Test
    fun `archived plans are omitted from the active list`() = runTest {
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA, fullBodyB, upperBodyA),
        )
        val viewModel = WorkoutPlansViewModel(repository, backgroundScope)

        val state = viewModel.uiState.first { it.activePlans.isNotEmpty() }
        assert(state.activePlans.map { it.plan } == listOf(fullBodyA, fullBodyB))
        assert(state.archivedPlans.isEmpty())
        assert(!state.showArchived)
        assert(state.hasArchivedPlans)
        assert(state.showArchivedPlansButton)
        assert(!state.hideArchivedPlansButton)
        assert(!state.showMyPlansHeading)
        assert(!state.showArchivedPlansHeading)
    }

    @Test
    fun `archiving a plan moves it to archived`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA, fullBodyB))
        val viewModel = WorkoutPlansViewModel(repository, backgroundScope)

        viewModel.uiState.first { it.activePlans.size == 2 }
        viewModel.onAction(WorkoutPlansAction.OnArchivePlan(fullBodyA))
        val state = viewModel.uiState.first { it.activePlans.size == 1 && it.hasArchivedPlans }
        assert(state.activePlans.single().plan == fullBodyB)
        assert(state.archivedPlans.isEmpty())
        assert(state.hasArchivedPlans)
        assert(!state.showArchived)
    }

    @Test
    fun `showing archived plans keeps active and archived in state`() = runTest {
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA, fullBodyB, upperBodyA),
        )
        val viewModel = WorkoutPlansViewModel(repository, backgroundScope)

        viewModel.uiState.first { it.hasArchivedPlans }
        viewModel.onAction(WorkoutPlansAction.OnShowArchivedPlans)
        val state = viewModel.uiState.first { it.showArchived }
        assert(state.activePlans.map { it.plan } == listOf(fullBodyA, fullBodyB))
        assert(state.archivedPlans.map { it.plan } == listOf(upperBodyA))
        assert(state.showMyPlansHeading)
        assert(state.showArchivedPlansHeading)
        assert(state.hideArchivedPlansButton)
        assert(!state.showArchivedPlansButton)
    }

    @Test
    fun `hiding archived plans turns the filter off`() = runTest {
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA, upperBodyA),
        )
        val viewModel = WorkoutPlansViewModel(repository, backgroundScope)

        viewModel.uiState.first { it.hasArchivedPlans }
        viewModel.onAction(WorkoutPlansAction.OnShowArchivedPlans)
        viewModel.uiState.first { it.showArchived }
        viewModel.onAction(WorkoutPlansAction.OnHideArchivedPlans)
        val state = viewModel.uiState.first { !it.showArchived }
        assert(state.archivedPlans.isEmpty())
        assert(state.hasArchivedPlans)
    }

    @Test
    fun `unarchiving a plan keeps the archived filter`() = runTest {
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA, upperBodyA),
        )
        val viewModel = WorkoutPlansViewModel(repository, backgroundScope)

        viewModel.uiState.first { it.hasArchivedPlans }
        viewModel.onAction(WorkoutPlansAction.OnShowArchivedPlans)
        viewModel.uiState.first { it.showArchived }
        viewModel.onAction(WorkoutPlansAction.OnUnarchivePlan(upperBodyA))
        val state = viewModel.uiState.first { it.archivedPlans.isEmpty() }
        assert(state.showArchived)
        assert(state.hideArchivedPlansButton)
        assert(!state.showArchivedPlansHeading)
        assert(!state.showArchivedPlansButton)
        assert(
            state.activePlans.map { it.plan } ==
                listOf(fullBodyA, upperBodyA.copy(isArchived = false)),
        )
    }
}
