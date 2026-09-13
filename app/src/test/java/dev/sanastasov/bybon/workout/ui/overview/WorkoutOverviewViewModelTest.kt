package dev.sanastasov.bybon.workout.ui.overview

import app.cash.turbine.test
import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.fullBodyA
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WorkoutOverviewViewModelTest {

    @Test
    fun `adjustments stay in memory until start persists the session`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutOverviewViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            val draft = awaitItem()!!
            assert(repository.workoutSessions().first().isEmpty())
            assert(draft.workoutSets.none { it.setState == SetState.InProgress })

            viewModel.onAction(WorkoutOverviewAction.OnIncreaseWorkout)
            val increased = awaitItem()!!
            assert(repository.workoutSessions().first().isEmpty())
            assert(
                increased.exercises.first().sets.first().reps ==
                    draft.exercises.first().sets.first().reps + 1,
            )
            assert(
                increased.exercises.first().sets.first().oneRm!! >
                    draft.exercises.first().sets.first().oneRm!!,
            )

            viewModel.onAction(WorkoutOverviewAction.OnStartWorkout)
            assert(viewModel.effects.first() == WorkoutOverviewEffect.NavigateToSession)
            val saved = repository.workoutSessions().first().single()
            assert(saved.exercises.first().sets.first().setState == SetState.InProgress)
            assert(
                saved.exercises.first().sets.first().reps ==
                    increased.exercises.first().sets.first().reps,
            )
        }
    }
}
