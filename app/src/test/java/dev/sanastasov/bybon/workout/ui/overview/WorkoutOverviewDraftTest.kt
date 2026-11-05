package dev.sanastasov.bybon.workout.ui.overview

import app.cash.turbine.test
import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.fullBodyA
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WorkoutOverviewDraftTest {

    @Test
    fun `save persists a not-started draft that overview reloads`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutOverviewViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            awaitItem()!!
            viewModel.onAction(WorkoutOverviewAction.OnIncreaseWorkout)
            val increased = awaitItem()!!
            viewModel.onAction(WorkoutOverviewAction.OnSaveDraft)
            val saved = repository.workoutSessions().first { it.isNotEmpty() }.single()
            assert(saved.state is WorkoutState.NotStarted)
            assert(saved.isDraft)
            assert(
                saved.exercises.first().sets.first().reps ==
                    increased.exercises.first().sets.first().reps,
            )
            assert(saved.workoutSets.none { it.setState == SetState.InProgress })
            cancelAndIgnoreRemainingEvents()
        }

        val reloaded = WorkoutOverviewViewModel(fullBodyA.id, repository, backgroundScope)
        val loaded = reloaded.uiState.first { it != null }!!
        assert(
            loaded.exercises.first().sets.first().reps ==
                repository.workoutSessions().first().single().exercises.first().sets.first().reps,
        )
        assert(loaded.session.state is WorkoutState.NotStarted)
    }

    @Test
    fun `saved draft is kept when the plan later changes`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutOverviewViewModel(fullBodyA.id, repository, backgroundScope)
        val originalCount = fullBodyA.sets.size

        viewModel.uiState.first { it != null }
        viewModel.onAction(WorkoutOverviewAction.OnSaveDraft)
        repository.workoutSessions().first { it.isNotEmpty() }

        repository.updatePlan(fullBodyA.id) { plan ->
            plan.copy(sets = plan.sets.take(1))
        }
        repository.workoutPlans().first { plans ->
            plans.first { it.id == fullBodyA.id }.sets.size == 1
        }

        val reloaded = WorkoutOverviewViewModel(fullBodyA.id, repository, backgroundScope)
        val loaded = reloaded.uiState.first { it != null }!!
        assert(loaded.exercises.size == originalCount)
        assert(repository.workoutPlans().first().single { it.id == fullBodyA.id }.sets.size == 1)
    }
}
