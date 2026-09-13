package dev.sanastasov.bybon.workout.ui.overview

import app.cash.turbine.test
import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import java.time.LocalDateTime
import kotlin.time.Duration
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
            assert(
                increased.exercises.first().sets.all {
                    it.weight == increased.exercises.first().sets.first().weight
                },
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

    @Test
    fun `reset restores previous session values at workout and exercise level`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutOverviewViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            val draft = awaitItem()!!

            viewModel.onAction(WorkoutOverviewAction.OnIncreaseWorkout)
            awaitItem()
            viewModel.onAction(WorkoutOverviewAction.OnResetWorkout)
            val workoutReset = awaitItem()!!
            assert(workoutReset.exercises == draft.exercises)

            viewModel.onAction(WorkoutOverviewAction.OnIncreaseExercise(draft.exercises.first()))
            val exerciseIncreased = awaitItem()!!
            assert(exerciseIncreased.exercises.first() != draft.exercises.first())
            assert(exerciseIncreased.exercises.drop(1) == draft.exercises.drop(1))

            viewModel.onAction(
                WorkoutOverviewAction.OnResetExercise(exerciseIncreased.exercises.first()),
            )
            val exerciseReset = awaitItem()!!
            assert(exerciseReset.exercises == draft.exercises)
        }
    }

    @Test
    fun `reset restores the last completed session after increasing`() = runTest {
        val previous = fullBodyA.toWorkoutSession().let { session ->
            session.copy(
                exercises = session.exercises.map { exercise ->
                    exercise.copy(
                        sets = exercise.sets.map { set ->
                            set.copy(
                                weight = Weight.kilograms(40),
                                reps = 9,
                                setState = SetState.Completed,
                            )
                        },
                    )
                },
                state = WorkoutState.Completed(
                    startedAt = LocalDateTime.of(2026, 1, 1, 12, 0),
                    duration = Duration.ZERO,
                ),
            )
        }
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialSessions = listOf(previous),
        )
        val viewModel = WorkoutOverviewViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            val draft = awaitItem()!!
            assert(draft.exercises.first().sets.first().weight == Weight.kilograms(40))
            assert(draft.exercises.first().sets.first().reps == 9)

            viewModel.onAction(WorkoutOverviewAction.OnIncreaseWorkout)
            val increased = awaitItem()!!
            assert(increased.exercises.first().sets.first() != draft.exercises.first().sets.first())

            viewModel.onAction(WorkoutOverviewAction.OnResetWorkout)
            val reset = awaitItem()!!
            assert(reset.exercises.first().sets.first().weight == Weight.kilograms(40))
            assert(reset.exercises.first().sets.first().reps == 9)
            assert(reset.exercises == draft.exercises)
        }
    }
}
