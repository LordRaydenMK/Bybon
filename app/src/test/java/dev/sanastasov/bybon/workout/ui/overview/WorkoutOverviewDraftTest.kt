package dev.sanastasov.bybon.workout.ui.overview

import app.cash.turbine.test
import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.domain.PreviousSetPerformance
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.toOverviewSession
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import dev.sanastasov.bybon.workout.ui.session.StartMode
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WorkoutOverviewDraftTest {

    @Test
    fun `save does not copy draft loads into previous performance`() = runTest {
        val completed = fullBodyA.toWorkoutSession().let { session ->
            session.copy(
                exercises = session.exercises.map { exercise ->
                    exercise.copy(
                        sets = exercise.sets.map { set ->
                            set.copy(
                                weight = Weight.kilograms(40),
                                reps = 9,
                                setState = SetState.Completed(),
                            )
                        },
                        warmupSets = exercise.warmupSets?.map { set ->
                            set.copy(setState = SetState.Completed())
                        },
                    )
                },
                startedAt = LocalDateTime.of(2026, 1, 1, 12, 0),
                duration = Duration.ZERO,
            )
        }
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialSessions = listOf(completed),
        )
        val viewModel = overviewViewModel(fullBodyA.id, repository, backgroundScope)
        val expectedPrevious = PreviousSetPerformance(Weight.kilograms(40), 9)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            val opened = awaitItem()!!
            assert(opened.exercises.first().sets.first().previous == expectedPrevious)

            viewModel.onAction(WorkoutOverviewAction.OnIncreaseWorkout)
            val increased = awaitItem()!!
            val increasedSet = increased.exercises.first().sets.first()
            assert(increasedSet.weight != Weight.kilograms(40) || increasedSet.reps != 9)
            assert(increasedSet.previous == expectedPrevious)

            viewModel.onAction(WorkoutOverviewAction.OnSaveDraft)
            val afterSave = awaitItem()!!
            val savedSet = afterSave.exercises.first().sets.first()
            assert(savedSet.weight == increasedSet.weight)
            assert(savedSet.reps == increasedSet.reps)
            assert(savedSet.previous == expectedPrevious)
            assert(afterSave.previousDate == "Previous: 1 Jan 2026")
        }

        val reloaded = overviewViewModel(fullBodyA.id, repository, backgroundScope)
        val loaded = reloaded.uiState.first { it != null }!!
        val loadedSet = loaded.exercises.first().sets.first()
        assert(loadedSet.previous == expectedPrevious)
        assert(loadedSet.weight != Weight.kilograms(40) || loadedSet.reps != 9)
        assert(loaded.previousDate == "Previous: 1 Jan 2026")
    }

    @Test
    fun `loaded draft rematches previous from completed work not its own loads`() = runTest {
        val completed = completedFullBodyA(LocalDateTime.of(2026, 1, 1, 12, 0))
        val draft = fullBodyA.toOverviewSession(completed).let { session ->
            session.copy(
                exercises = session.exercises.map { exercise ->
                    exercise.copy(
                        sets = exercise.sets.map { set ->
                            val current = set.copy(weight = Weight.kilograms(55), reps = 12)
                            current.copy(
                                previous = PreviousSetPerformance(current.weight, current.reps),
                            )
                        },
                    )
                },
                startedAt = LocalDateTime.of(2026, 2, 1, 12, 0),
            )
        }
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialSessions = listOf(completed, draft),
        )
        val viewModel = overviewViewModel(fullBodyA.id, repository, backgroundScope)
        val loaded = viewModel.uiState.first { it != null }!!
        val set = loaded.exercises.first().sets.first()

        assert(set.weight == Weight.kilograms(55))
        assert(set.reps == 12)
        assert(set.previous == PreviousSetPerformance(Weight.kilograms(40), 9))
        assert(loaded.previousDate == "Previous: 1 Jan 2026")
    }

    @Test
    fun `save persists a not-started draft that overview reloads`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = overviewViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            awaitItem()!!
            viewModel.onAction(WorkoutOverviewAction.OnIncreaseWorkout)
            val increased = awaitItem()!!
            assert(!increased.canDiscardDraft)
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

        val reloaded = overviewViewModel(fullBodyA.id, repository, backgroundScope)
        val loaded = reloaded.uiState.first { it != null }!!
        assert(
            loaded.exercises.first().sets.first().reps ==
                repository.workoutSessions().first().single().exercises.first().sets.first().reps,
        )
        assert(loaded.session.state is WorkoutState.NotStarted)
        assert(loaded.canDiscardDraft)
    }

    @Test
    fun `saved draft is kept when the plan later changes`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = overviewViewModel(fullBodyA.id, repository, backgroundScope)
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

        val reloaded = overviewViewModel(fullBodyA.id, repository, backgroundScope)
        val loaded = reloaded.uiState.first { it != null }!!
        assert(loaded.exercises.size == originalCount)
        assert(repository.workoutPlans().first().single { it.id == fullBodyA.id }.sets.size == 1)
    }

    @Test
    fun `discarding a saved draft deletes it and leaves overview`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = overviewViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.first { it != null }
        viewModel.onAction(WorkoutOverviewAction.OnSaveDraft)
        repository.workoutSessions().first { it.isNotEmpty() }

        viewModel.onAction(WorkoutOverviewAction.OnDiscardDraft)

        assert(viewModel.effects.first() == WorkoutOverviewEffect.NavigateBack)
        assert(repository.workoutSessions().first().isEmpty())
    }

    @Test
    fun `discard is offered only when a draft is already saved`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = overviewViewModel(fullBodyA.id, repository, backgroundScope)

        assert(!viewModel.uiState.first { it != null }!!.canDiscardDraft)

        viewModel.onAction(WorkoutOverviewAction.OnSaveDraft)
        repository.workoutSessions().first { it.isNotEmpty() }

        assert(viewModel.uiState.first { it?.canDiscardDraft == true }!!.canDiscardDraft)
    }

    @Test
    fun `a loaded draft offers discard`() = runTest {
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialSessions = listOf(fullBodyA.toOverviewSession()),
        )
        val viewModel = overviewViewModel(fullBodyA.id, repository, backgroundScope)

        assert(viewModel.uiState.first { it != null }!!.canDiscardDraft)
    }

    @Test
    fun `starting a saved draft asks the session screen to start it`() = runTest {
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialSessions = listOf(fullBodyA.toOverviewSession()),
        )
        val viewModel = overviewViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.first { it != null }
        viewModel.onAction(WorkoutOverviewAction.OnStartWorkout)

        assert(
            viewModel.effects.first() ==
                WorkoutOverviewEffect.NavigateToSession(StartMode.StartDraft),
        )
        assert(repository.workoutSessions().first().single().isDraft)
    }
}

private fun completedFullBodyA(startedAt: LocalDateTime) =
    fullBodyA.toWorkoutSession().let { session ->
        session.copy(
            exercises = session.exercises.map { exercise ->
                exercise.copy(
                    sets = exercise.sets.map { set ->
                        set.copy(
                            weight = Weight.kilograms(40),
                            reps = 9,
                            setState = SetState.Completed(),
                        )
                    },
                    warmupSets = exercise.warmupSets?.map {
                        it.copy(setState = SetState.Completed())
                    },
                )
            },
            startedAt = startedAt,
            duration = Duration.ZERO,
        )
    }
