package dev.sanastasov.bybon.workout.ui.plans

import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import dev.sanastasov.bybon.workout.domain.upperBodyA
import dev.sanastasov.bybon.workout.ui.overview.WorkoutOverviewAction
import dev.sanastasov.bybon.workout.ui.overview.WorkoutOverviewViewModel
import dev.sanastasov.bybon.workout.ui.session.WorkoutSessionAction
import dev.sanastasov.bybon.workout.ui.session.WorkoutSessionViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WorkoutPlansViewModelTest {

    @Test
    fun `starting an inactive plan opens the overview`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutPlansViewModel(repository, backgroundScope)

        viewModel.uiState.first { it.unarchivedPlans.isNotEmpty() }
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

        viewModel.uiState.first { state -> state.unarchivedPlans.any { it.isActive } }
        viewModel.onAction(WorkoutPlansAction.OnStartPlan(fullBodyA))
        assert(viewModel.effects.first() == WorkoutPlanEffect.OpenSession(fullBodyA))
    }

    @Test
    fun `a started session stays active without an in-progress set`() = runTest {
        val started = fullBodyA.toWorkoutSession()
        val paused = started.copy(
            state = WorkoutState.InProgress,
            exercises = started.exercises.mapIndexed { exerciseIndex, exercise ->
                if (exerciseIndex != 0) {
                    exercise
                } else {
                    exercise.copy(
                        warmupSets = exercise.warmupSets?.mapIndexed { index, set ->
                            set.copy(
                                setState = if (index == 0) {
                                    SetState.Completed
                                } else {
                                    SetState.NotStated
                                },
                            )
                        },
                    )
                }
            },
        )
        assert(paused.workoutSets.none { it.setState == SetState.InProgress })
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialSessions = listOf(paused),
        )
        val viewModel = WorkoutPlansViewModel(repository, backgroundScope)

        val state = viewModel.uiState.first { s -> s.unarchivedPlans.any { it.isActive } }
        assert(state.unarchivedPlans.single().isActive)
        viewModel.onAction(WorkoutPlansAction.OnStartPlan(fullBodyA))
        assert(viewModel.effects.first() == WorkoutPlanEffect.OpenSession(fullBodyA))
    }

    @Test
    fun `completing sets from a started workout keeps the plan active`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val plans = WorkoutPlansViewModel(repository, backgroundScope)
        val overview = WorkoutOverviewViewModel(fullBodyA.id, repository, backgroundScope)

        overview.uiState.first { it != null }
        overview.onAction(WorkoutOverviewAction.OnStartWorkout)
        overview.effects.first()

        val sessionVm = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)
        val session = sessionVm.uiState.first { it != null }!!
        val bench = session.exercises.first()
        sessionVm.onAction(WorkoutSessionAction.OnCompleteSet(bench, 0, isWarmup = true))
        sessionVm.onAction(WorkoutSessionAction.OnCompleteSet(bench, 1, isWarmup = true))
        sessionVm.uiState.first { current ->
            current?.exercises?.first()?.warmupSets?.get(1)?.setState == SetState.Completed
        }

        val state = plans.uiState.first { s -> s.unarchivedPlans.any { it.isActive } }
        assert(state.unarchivedPlans.single().isActive)
        plans.onAction(WorkoutPlansAction.OnStartPlan(fullBodyA))
        assert(plans.effects.first() == WorkoutPlanEffect.OpenSession(fullBodyA))
    }

    @Test
    fun `editing a plan opens the edit screen`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutPlansViewModel(repository, backgroundScope)

        viewModel.uiState.first { it.unarchivedPlans.isNotEmpty() }
        viewModel.onAction(WorkoutPlansAction.OnEditPlan(fullBodyA))
        assert(viewModel.effects.first() == WorkoutPlanEffect.OpenEditPlan(fullBodyA))
    }

    @Test
    fun `archived plans are omitted from the unarchived list`() = runTest {
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA, fullBodyB, upperBodyA),
        )
        val viewModel = WorkoutPlansViewModel(repository, backgroundScope)

        val state = viewModel.uiState.first { it.unarchivedPlans.isNotEmpty() }
        assert(state.unarchivedPlans.map { it.plan } == listOf(fullBodyA, fullBodyB))
        assert(state.archivedPlans.map { it.plan } == listOf(upperBodyA))
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

        viewModel.uiState.first { it.unarchivedPlans.size == 2 }
        viewModel.onAction(WorkoutPlansAction.OnArchivePlan(fullBodyA))
        val state = viewModel.uiState.first { it.unarchivedPlans.size == 1 }
        assert(state.unarchivedPlans.single().plan == fullBodyB)
        assert(state.archivedPlans.single().plan == fullBodyA.copy(isArchived = true))
        assert(state.hasArchivedPlans)
        assert(!state.showArchived)
    }

    @Test
    fun `showing archived plans keeps unarchived and archived in state`() = runTest {
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA, fullBodyB, upperBodyA),
        )
        val viewModel = WorkoutPlansViewModel(repository, backgroundScope)

        viewModel.uiState.first { it.hasArchivedPlans }
        viewModel.onAction(WorkoutPlansAction.OnShowArchivedPlans)
        val state = viewModel.uiState.first { it.showArchived }
        assert(state.unarchivedPlans.map { it.plan } == listOf(fullBodyA, fullBodyB))
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
        assert(state.archivedPlans.map { it.plan } == listOf(upperBodyA))
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
            state.unarchivedPlans.map { it.plan } ==
                listOf(fullBodyA, upperBodyA.copy(isArchived = false)),
        )
    }
}
