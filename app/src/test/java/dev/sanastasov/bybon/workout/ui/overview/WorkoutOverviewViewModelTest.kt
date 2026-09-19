package dev.sanastasov.bybon.workout.ui.overview

import app.cash.turbine.test
import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.formatRestClock
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
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
                increased.exercises.first().sets.first().oneRm >
                    draft.exercises.first().sets.first().oneRm,
            )
            assert(
                increased.exercises.first().sets.all {
                    it.weight == increased.exercises.first().sets.first().weight
                },
            )

            viewModel.onAction(WorkoutOverviewAction.OnStartWorkout)
            assert(viewModel.effects.first() == WorkoutOverviewEffect.NavigateToSession)
            val saved = repository.workoutSessions().first().single()
            assert(saved.state == WorkoutState.InProgress)
            assert(saved.exercises.first().warmupSets!!.first().setState == SetState.InProgress)
            assert(saved.exercises.first().sets.first().setState == SetState.NotStated)
            assert(
                saved.exercises.first().sets.first().reps ==
                    increased.exercises.first().sets.first().reps,
            )
            assert(saved.exercises.first().warmupSets?.size == 3)
        }
    }

    @Test
    fun `converting set one to warmup stays in memory until start`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutOverviewViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            val draft = awaitItem()!!
            assert(draft.exercises.first().warmupSets?.size == 3)
            assert(draft.exercises.first().sets.size == 3)

            viewModel.onAction(WorkoutOverviewAction.OnConvertToWarmup(draft.exercises.first()))
            val converted = awaitItem()!!
            assert(repository.workoutSessions().first().isEmpty())
            assert(converted.exercises.first().warmupSets?.size == 4)
            assert(converted.exercises.first().sets.size == 2)

            viewModel.onAction(
                WorkoutOverviewAction.OnConvertToWorkSet(converted.exercises.first()),
            )
            val restored = awaitItem()!!
            assert(restored.exercises.first().warmupSets?.size == 3)
            assert(restored.exercises.first().sets.size == 3)

            viewModel.onAction(WorkoutOverviewAction.OnAddSet(restored.exercises.first()))
            val added = awaitItem()!!
            assert(added.exercises.first().warmupSets?.size == 3)
            assert(added.exercises.first().sets.size == 4)

            viewModel.onAction(WorkoutOverviewAction.RemoveLastSet(added.exercises.first()))
            val removed = awaitItem()!!
            assert(removed.exercises.first().sets.size == 3)
            assert(removed.exercises.first().warmupSets?.size == 3)
        }
    }

    @Test
    fun `reset restores previous performance at exercise and set level`() = runTest {
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
                        warmupSets = exercise.warmupSets?.map { set ->
                            set.copy(setState = SetState.Completed)
                        },
                    )
                },
                startedAt = LocalDateTime.of(2026, 1, 1, 12, 0),
                duration = Duration.ZERO,
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

            viewModel.onAction(WorkoutOverviewAction.OnIncreaseExercise(draft.exercises.first()))
            val exerciseIncreased = awaitItem()!!
            assert(exerciseIncreased.exercises.first() != draft.exercises.first())
            assert(exerciseIncreased.exercises.drop(1) == draft.exercises.drop(1))

            viewModel.onAction(
                WorkoutOverviewAction.OnResetExercise(exerciseIncreased.exercises.first()),
            )
            val exerciseReset = awaitItem()!!
            assert(exerciseReset.exercises.first().sets == draft.exercises.first().sets)

            viewModel.onAction(WorkoutOverviewAction.OnIncreaseExercise(draft.exercises.first()))
            val increasedAgain = awaitItem()!!
            viewModel.onAction(
                WorkoutOverviewAction.OnResetSet(
                    increasedAgain.exercises.first(),
                    index = 0,
                    isWarmup = false,
                ),
            )
            val setReset = awaitItem()!!
            assert(setReset.exercises.first().sets.first().weight == Weight.kilograms(40))
            assert(setReset.exercises.first().sets.first().reps == 9)
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
                        warmupSets = exercise.warmupSets?.map { set ->
                            set.copy(setState = SetState.Completed)
                        },
                    )
                },
                startedAt = LocalDateTime.of(2026, 1, 1, 12, 0),
                duration = Duration.ZERO,
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

            viewModel.onAction(
                WorkoutOverviewAction.OnResetExercise(increased.exercises.first()),
            )
            val reset = awaitItem()!!
            assert(reset.exercises.first().sets.first().weight == Weight.kilograms(40))
            assert(reset.exercises.first().sets.first().reps == 9)
        }
    }

    @Test
    fun `overview session exposes rest timers on work sets only`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutOverviewViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            val draft = awaitItem()!!
            assert(draft.exercises.first().restAfterWorkSet == 2.minutes)
            assert(draft.exercises.first().restAfterWorkSet.formatRestClock() == "2:00")
            assert(
                draft.exercises.first { it.id == "leg-curl" }.restAfterWorkSet.formatRestClock() ==
                    "1:30",
            )
            assert(
                draft.exercises.first {
                    it.id == "skullcrusher-db"
                }.restAfterWorkSet.formatRestClock() ==
                    "1:00",
            )
            assert(draft.exercises.first().warmupSets != null)
        }
    }
}
