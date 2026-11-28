package dev.sanastasov.bybon.workout.ui.plans

import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import dev.sanastasov.bybon.workout.domain.toOverviewSession
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import dev.sanastasov.bybon.workout.domain.upperBodyA
import dev.sanastasov.bybon.workout.ui.overview.WorkoutOverviewAction
import dev.sanastasov.bybon.workout.ui.overview.overviewViewModel
import dev.sanastasov.bybon.workout.ui.session.WorkoutSessionAction
import dev.sanastasov.bybon.workout.ui.session.sessionViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WorkoutPlansViewModelTest {

    @Test
    fun `starting an inactive plan opens the overview`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = plansViewModel(repository, backgroundScope)

        viewModel.uiState.first { it.unarchivedPlans.isNotEmpty() }
        viewModel.onAction(WorkoutPlansAction.OnStartPlan(fullBodyA))
        assert(viewModel.effects.first() == WorkoutPlanEffect.OpenOverview(fullBodyA))
    }

    @Test
    fun `starting a plan with a draft opens the overview`() = runTest {
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialSessions = listOf(fullBodyA.toOverviewSession()),
        )
        val viewModel = plansViewModel(repository, backgroundScope)

        val state = viewModel.uiState.first { s -> s.unarchivedPlans.any { it.isDraft } }
        assert(state.unarchivedPlans.single().isDraft)
        assert(!state.unarchivedPlans.single().isActive)
        viewModel.onAction(WorkoutPlansAction.OnStartPlan(fullBodyA))
        assert(viewModel.effects.first() == WorkoutPlanEffect.OpenOverview(fullBodyA))
    }

    @Test
    fun `discarding a draft deletes the saved session`() = runTest {
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialSessions = listOf(fullBodyA.toOverviewSession()),
        )
        val viewModel = plansViewModel(repository, backgroundScope)

        viewModel.uiState.first { s -> s.unarchivedPlans.any { it.isDraft } }
        viewModel.onAction(WorkoutPlansAction.OnDiscardDraft(fullBodyA))

        val state = viewModel.uiState.first { s -> s.unarchivedPlans.none { it.isDraft } }
        assert(state.unarchivedPlans.single().startActionLabel == "Start Workout")
        assert(repository.workoutSessions().first().isEmpty())
    }

    @Test
    fun `starting an active plan opens the session`() = runTest {
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialSessions = listOf(fullBodyA.toWorkoutSession()),
        )
        val viewModel = plansViewModel(repository, backgroundScope)

        viewModel.uiState.first { state -> state.unarchivedPlans.any { it.isActive } }
        viewModel.onAction(WorkoutPlansAction.OnStartPlan(fullBodyA))
        assert(viewModel.effects.first() == WorkoutPlanEffect.OpenSession(fullBodyA))
    }

    @Test
    fun `a started session stays active without an in-progress set`() = runTest {
        val started = fullBodyA.toWorkoutSession()
        val paused = started.copy(
            exercises = started.exercises.mapIndexed { exerciseIndex, exercise ->
                if (exerciseIndex != 0) {
                    exercise
                } else {
                    exercise.copy(
                        warmupSets = exercise.warmupSets?.mapIndexed { index, set ->
                            set.copy(
                                setState = if (index == 0) {
                                    SetState.Completed()
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
        val viewModel = plansViewModel(repository, backgroundScope)

        val state = viewModel.uiState.first { s -> s.unarchivedPlans.any { it.isActive } }
        assert(state.unarchivedPlans.single().isActive)
        viewModel.onAction(WorkoutPlansAction.OnStartPlan(fullBodyA))
        assert(viewModel.effects.first() == WorkoutPlanEffect.OpenSession(fullBodyA))
    }

    @Test
    fun `completing sets from a started workout keeps the plan active`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val plans = plansViewModel(repository, backgroundScope)
        val overview = overviewViewModel(fullBodyA.id, repository, backgroundScope)

        overview.uiState.first { it != null }
        overview.onAction(WorkoutOverviewAction.OnStartWorkout)
        overview.effects.first()

        val sessionVm = sessionViewModel(fullBodyA.id, repository, backgroundScope)
        val session = sessionVm.uiState.first { it != null }!!
        val bench = session.exercises.first()
        sessionVm.onAction(WorkoutSessionAction.OnCompleteSet(bench, 0, isWarmup = true))
        sessionVm.onAction(WorkoutSessionAction.OnCompleteSet(bench, 1, isWarmup = true))
        sessionVm.uiState.first { current ->
            current?.exercises?.first()?.warmupSets?.get(1)?.setState is SetState.Completed
        }

        val state = plans.uiState.first { s -> s.unarchivedPlans.any { it.isActive } }
        assert(state.unarchivedPlans.single().isActive)
        plans.onAction(WorkoutPlansAction.OnStartPlan(fullBodyA))
        assert(plans.effects.first() == WorkoutPlanEffect.OpenSession(fullBodyA))
    }

    @Test
    fun `canceling a workout clears the active plan`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val sessionVm = sessionViewModel(fullBodyA.id, repository, backgroundScope)
        sessionVm.uiState.first { it != null }
        val plans = plansViewModel(repository, backgroundScope)
        plans.uiState.first { s -> s.unarchivedPlans.any { it.isActive } }

        sessionVm.onAction(WorkoutSessionAction.OnCancelWorkout)

        val state = plans.uiState.first { s -> s.unarchivedPlans.none { it.isActive } }
        assert(state.unarchivedPlans.none { it.isActive })
        plans.onAction(WorkoutPlansAction.OnStartPlan(fullBodyA))
        assert(plans.effects.first() == WorkoutPlanEffect.OpenOverview(fullBodyA))
    }

    @Test
    fun `editing a plan opens the edit screen`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = plansViewModel(repository, backgroundScope)

        viewModel.uiState.first { it.unarchivedPlans.isNotEmpty() }
        viewModel.onAction(WorkoutPlansAction.OnEditPlan(fullBodyA))
        assert(viewModel.effects.first() == WorkoutPlanEffect.OpenEditPlan(fullBodyA))
    }

    @Test
    fun `duplicating a plan copies its exercises after the original`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA, fullBodyB))
        val viewModel = plansViewModel(repository, backgroundScope)

        viewModel.uiState.first { it.unarchivedPlans.size == 2 }
        viewModel.onAction(WorkoutPlansAction.OnDuplicatePlan(fullBodyA))

        val state = viewModel.uiState.first { it.unarchivedPlans.size == 3 }
        val plans = state.unarchivedPlans.map { it.plan }
        val copy = plans[1]
        assert(plans.map { it.id } == listOf(fullBodyA.id, copy.id, fullBodyB.id))
        assert(copy.name == "Copy of ${fullBodyA.name}")
        assert(copy.id != fullBodyA.id)
        assert(copy.description == fullBodyA.description)
        assert(copy.sets == fullBodyA.sets)
        assert(!copy.isArchived)
        assert(plans[0] == fullBodyA)
        assert(state.unarchivedPlans[1] is WorkoutPlanUi.Idle)
        assert(state.unarchivedPlans[1].lastExecutedLabel == null)
    }

    @Test
    fun `duplicating an active plan does not copy its workout`() = runTest {
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA, fullBodyB),
            initialSessions = listOf(fullBodyA.toWorkoutSession()),
        )
        val viewModel = plansViewModel(repository, backgroundScope)

        viewModel.uiState.first { state -> state.unarchivedPlans.any { it.isActive } }
        viewModel.onAction(WorkoutPlansAction.OnDuplicatePlan(fullBodyA))

        val state = viewModel.uiState.first { it.unarchivedPlans.size == 3 }
        assert(state.unarchivedPlans[0].isActive)
        assert(state.unarchivedPlans[0].plan == fullBodyA)
        assert(state.unarchivedPlans[1].plan.name == "Copy of ${fullBodyA.name}")
        assert(!state.unarchivedPlans[1].isActive)
        assert(!state.unarchivedPlans[1].isDraft)
        assert(repository.workoutSessions().first().map { it.planId } == listOf(fullBodyA.id))
    }

    @Test
    fun `an active plan is listed first among unarchived plans`() = runTest {
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA, fullBodyB),
            initialSessions = listOf(fullBodyB.toWorkoutSession()),
        )
        val viewModel = plansViewModel(repository, backgroundScope)

        val state = viewModel.uiState.first { s -> s.unarchivedPlans.any { it.isActive } }
        assert(state.unarchivedPlans.map { it.plan.id } == listOf(fullBodyB.id, fullBodyA.id))
        val active = state.unarchivedPlans.first() as WorkoutPlanUi.Active
        assert(active.activeExerciseId == fullBodyB.sets.first().exercise.id)
    }

    @Test
    fun `archived plans are omitted from the unarchived list`() = runTest {
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA, fullBodyB, upperBodyA),
        )
        val viewModel = plansViewModel(repository, backgroundScope)

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
        val viewModel = plansViewModel(repository, backgroundScope)

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
        val viewModel = plansViewModel(repository, backgroundScope)

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
        val viewModel = plansViewModel(repository, backgroundScope)

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
        val viewModel = plansViewModel(repository, backgroundScope)

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

    @Test
    fun `move plan up swaps idle plans`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA, fullBodyB))
        val viewModel = plansViewModel(repository, backgroundScope)

        viewModel.uiState.first { it.unarchivedPlans.size == 2 }
        viewModel.onAction(WorkoutPlansAction.OnMovePlanUp(fullBodyB))
        val state = viewModel.uiState.first { current ->
            current.unarchivedPlans.map { it.plan.id } == listOf(fullBodyB.id, fullBodyA.id)
        }

        assert(state.unarchivedPlans.map { it.plan.id } == listOf(fullBodyB.id, fullBodyA.id))
    }

    @Test
    fun `moving an idle plan keeps the active plan on top`() = runTest {
        val legacy = upperBodyA.copy(isArchived = false)
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA, fullBodyB, legacy),
            initialSessions = listOf(fullBodyA.toWorkoutSession()),
        )
        val viewModel = plansViewModel(repository, backgroundScope)

        viewModel.uiState.first { it.unarchivedPlans.any { plan -> plan.isActive } }
        viewModel.onAction(WorkoutPlansAction.OnMovePlanUp(legacy))
        val state = viewModel.uiState.first { current ->
            current.unarchivedPlans.map { it.plan.id } ==
                listOf(fullBodyA.id, legacy.id, fullBodyB.id)
        }

        val order = listOf(fullBodyA.id, legacy.id, fullBodyB.id)
        assert(state.unarchivedPlans.first() is WorkoutPlanUi.Active)
        assert(state.unarchivedPlans.map { it.plan.id } == order)
    }

    @Test
    fun `a draft stays above idle plans when an idle plan moves`() = runTest {
        val legacy = upperBodyA.copy(isArchived = false)
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA, fullBodyB, legacy),
            initialSessions = listOf(fullBodyA.toOverviewSession()),
        )
        val viewModel = plansViewModel(repository, backgroundScope)

        viewModel.uiState.first { it.unarchivedPlans.any { plan -> plan.isDraft } }
        viewModel.onAction(WorkoutPlansAction.OnMovePlanUp(legacy))
        val state = viewModel.uiState.first { current ->
            current.unarchivedPlans.map { it.plan.id } ==
                listOf(fullBodyA.id, legacy.id, fullBodyB.id)
        }

        assert(state.unarchivedPlans.first() is WorkoutPlanUi.Draft)
        assert(
            state.unarchivedPlans.map { it.plan.id } ==
                listOf(fullBodyA.id, legacy.id, fullBodyB.id),
        )
    }
}
