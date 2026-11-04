package dev.sanastasov.bybon.workout.ui.session

import app.cash.turbine.test
import dev.sanastasov.bybon.test.BackgroundFailures
import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.data.completedExercise
import dev.sanastasov.bybon.workout.data.completedSession
import dev.sanastasov.bybon.workout.domain.ExerciseSet
import dev.sanastasov.bybon.workout.domain.PreviousSetPerformance
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.catalogExercise
import dev.sanastasov.bybon.workout.domain.catalogExercises
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WorkoutSessionViewModelExerciseEditingTest {

    @Test
    fun `add exercise opens the library with session exercise ids`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.effects.test {
            val session = viewModel.uiState.first { it != null }!!
            viewModel.onAction(WorkoutSessionAction.OnAddExercise)
            assert(
                awaitItem() == WorkoutSessionEffect.OpenExerciseLibrary(
                    session.exercises.map { it.id },
                ),
            )
        }
    }

    @Test
    fun `opening details emits the exercise id`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.effects.test {
            val session = viewModel.uiState.first { it != null }!!
            viewModel.onAction(
                WorkoutSessionAction.OnOpenExerciseDetails(session.exercises.first().id),
            )
            assert(
                awaitItem() == WorkoutSessionEffect.OpenExerciseDetails(
                    session.exercises.first().id,
                ),
            )
        }
    }

    @Test
    fun `picked exercise is appended to the session`() = runTest {
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialExercises = catalogExercises,
        )
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            val session = awaitItem()!!
            viewModel.onAction(WorkoutSessionAction.OnExercisePicked("incline-db-curl"))
            val added = awaitItem()!!
            assert(added.exercises.last().id == "incline-db-curl")
            assert(
                added.exercises.last().exerciseDefinition ==
                    catalogExercise("incline-db-curl"),
            )
            assert(
                repository.exercises().first().any { it.id == added.exercises.last().id },
            )
            assert(added.exercises.last().sets.size == 3)
            assert(added.exercises.last().warmupSets == null)
            assert(added.exercises.last().repRange == 8..12)
            assert(added.exercises.dropLast(1) == session.exercises)
            assert(added.exercises.last().sets.all { it.setState == SetState.NotStated })
            assert(added.exercises.first().warmupSets!!.first().setState == SetState.InProgress)
            assert(repository.workoutPlans().first() == listOf(fullBodyA))
        }
    }

    @Test
    fun `picked exercise previous performance comes from the last time it was logged`() = runTest {
        val previousCurl = completedSession(
            planId = fullBodyB.id.id,
            planName = fullBodyB.name,
            startedAt = LocalDateTime.of(2026, 8, 13, 12, 0),
            exercises = listOf(
                completedExercise("incline-db-curl", 12f to 10, 12f to 9, 12f to 8),
            ),
        )
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialSessions = listOf(previousCurl),
            initialExercises = catalogExercises,
        )
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            awaitItem()!!
            viewModel.onAction(WorkoutSessionAction.OnExercisePicked("incline-db-curl"))
            val added = awaitItem()!!
            val curl = added.exercises.last()
            assert(curl.sets[0].previous == PreviousSetPerformance(Weight.kilograms(12), 10))
            assert(curl.sets[0].weight == Weight.kilograms(12))
            assert(curl.sets.map { it.reps } == listOf(10, 9, 8))
            assert(curl.warmupSets == null)
        }
    }

    @Test
    fun `unknown picked exercise is rejected`() = runTest {
        val failures = BackgroundFailures(this)
        val viewModel = WorkoutSessionViewModel(
            fullBodyA.id,
            FakeWorkoutsRepository(
                initialPlans = listOf(fullBodyA),
                initialExercises = catalogExercises,
            ),
            failures.scope,
        )
        viewModel.uiState.first { it != null }
        failures.expectFailure("Exercise missing is not in the repository") {
            viewModel.onAction(WorkoutSessionAction.OnExercisePicked("missing"))
        }
    }

    @Test
    fun `duplicate picked exercise is rejected`() = runTest {
        val failures = BackgroundFailures(this)
        val viewModel = WorkoutSessionViewModel(
            fullBodyA.id,
            FakeWorkoutsRepository(
                initialPlans = listOf(fullBodyA),
                initialExercises = catalogExercises,
            ),
            failures.scope,
        )
        viewModel.uiState.first { it != null }
        failures.expectFailure("Exercise bench-press is already in the session") {
            viewModel.onAction(WorkoutSessionAction.OnExercisePicked("bench-press"))
        }
    }

    @Test
    fun `removing an incomplete exercise persists the session without it`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            val session = awaitItem()!!
            val removed = session.exercises.first { it.id == "seated-leg-curl" }

            viewModel.onAction(WorkoutSessionAction.OnRemoveExercise(removed))
            val updated = awaitItem()!!
            assert(updated.exercises.none { it.id == "seated-leg-curl" })
            assert(updated.exercises.size == session.exercises.size - 1)
            assert(updated.exercises.first().warmupSets!!.first().setState == SetState.InProgress)
            assert(repository.workoutPlans().first() == listOf(fullBodyA))
        }
    }

    @Test
    fun `removing the in-progress exercise starts the next one`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            val session = awaitItem()!!

            viewModel.onAction(WorkoutSessionAction.OnRemoveExercise(session.exercises.first()))
            val updated = awaitItem()!!
            assert(updated.exercises.none { it.id == "bench-press" })
            assert(updated.exercises.first().id == "squat")
            assert(updated.exercises.first().warmupSets!!.first().setState == SetState.InProgress)
        }
    }

    @Test
    fun `removing a completed exercise is rejected`() = runTest {
        val session = twoExerciseSession(
            firstState = SetState.Completed(),
            secondState = SetState.InProgress,
        )
        val failures = BackgroundFailures(this)
        val viewModel = WorkoutSessionViewModel(
            fullBodyA.id,
            FakeWorkoutsRepository(
                initialPlans = listOf(fullBodyA),
                initialSessions = listOf(session),
            ),
            failures.scope,
        )
        val current = viewModel.uiState.first { it != null }!!
        failures.expectFailure("Cannot remove completed exercise bench-press") {
            viewModel.onAction(WorkoutSessionAction.OnRemoveExercise(current.exercises.first()))
        }
    }

    @Test
    fun `removing an unknown exercise is rejected`() = runTest {
        val failures = BackgroundFailures(this)
        val viewModel = WorkoutSessionViewModel(
            fullBodyA.id,
            FakeWorkoutsRepository(initialPlans = listOf(fullBodyA)),
            failures.scope,
        )
        val current = viewModel.uiState.first { it != null }!!
        val missing = current.exercises.first().copy(
            exerciseDefinition = catalogExercise("incline-db-curl"),
        )
        failures.expectFailure("Exercise incline-db-curl is not in the session") {
            viewModel.onAction(WorkoutSessionAction.OnRemoveExercise(missing))
        }
    }

    @Test
    fun `removing the last remaining exercise is rejected`() = runTest {
        val bench = catalogExercise("bench-press")
        val session = WorkoutSession(
            planId = fullBodyA.id,
            planName = fullBodyA.name,
            note = null,
            exercises = listOf(
                WorkoutExercise(
                    exerciseDefinition = bench,
                    repRange = 8..10,
                    sets = listOf(
                        ExerciseSet(bench, Weight.kilograms(50), 8, SetState.InProgress),
                    ),
                ),
            ),
            startedAt = LocalDateTime.of(2026, 1, 1, 12, 0),
        )
        val failures = BackgroundFailures(this)
        val viewModel = WorkoutSessionViewModel(
            fullBodyA.id,
            FakeWorkoutsRepository(
                initialPlans = listOf(fullBodyA),
                initialSessions = listOf(session),
            ),
            failures.scope,
        )
        val current = viewModel.uiState.first { it != null }!!
        failures.expectFailure("Cannot remove last exercise from the session") {
            viewModel.onAction(WorkoutSessionAction.OnRemoveExercise(current.exercises.single()))
        }
    }

    @Test
    fun `removing a later exercise scrolls to the previous page`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.effects.test {
            val session = viewModel.uiState.first { it != null }!!
            val removed = session.exercises.first { it.id == "seated-leg-curl" }
            val expectedPage = session.exercises.indexOfFirst { it.id == removed.id } - 1

            viewModel.onAction(WorkoutSessionAction.OnRemoveExercise(removed))
            assert(awaitItem() == WorkoutSessionEffect.ScrollToExercise(expectedPage))
            expectNoEvents()
        }
    }

    @Test
    fun `removing the last exercise scrolls to the previous page`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.effects.test {
            val session = viewModel.uiState.first { it != null }!!
            val removed = session.exercises.last()

            viewModel.onAction(WorkoutSessionAction.OnRemoveExercise(removed))
            assert(
                awaitItem() ==
                    WorkoutSessionEffect.ScrollToExercise(session.exercises.lastIndex - 1),
            )
            expectNoEvents()
        }
    }

    @Test
    fun `removing the first exercise stays on the first page`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.effects.test {
            val session = viewModel.uiState.first { it != null }!!

            viewModel.onAction(WorkoutSessionAction.OnRemoveExercise(session.exercises.first()))
            assert(awaitItem() == WorkoutSessionEffect.ScrollToExercise(0))
            expectNoEvents()
        }
    }

    @Test
    fun `removing the last incomplete exercise completes the workout`() = runTest {
        val session = twoExerciseSession(
            firstState = SetState.Completed(),
            secondState = SetState.InProgress,
        )
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialSessions = listOf(session),
        )
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.effects.test {
            val current = viewModel.uiState.first { it != null }!!
            viewModel.onAction(WorkoutSessionAction.OnRemoveExercise(current.exercises.last()))
            assert(awaitItem() == WorkoutSessionEffect.StopService)
            assert(awaitItem() == WorkoutSessionEffect.NavigateToSummary(session.id))
            expectNoEvents()
        }
        val stored = repository.workoutSessions().first().single()
        assert(stored.state is WorkoutState.Completed)
        assert(stored.exercises.single().id == "bench-press")
        assert(repository.workoutPlans().first() == listOf(fullBodyA))
    }

    @Test
    fun `the next session uses the original plan after session add and remove`() = runTest {
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialExercises = catalogExercises,
        )
        val first = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)
        first.uiState.first { it != null }
        first.onAction(WorkoutSessionAction.OnExercisePicked("incline-db-curl"))
        val added = first.uiState.first { session ->
            session?.exercises?.last()?.id == "incline-db-curl"
        }!!
        val removed = added.exercises.first { it.id == "seated-leg-curl" }
        first.onAction(WorkoutSessionAction.OnRemoveExercise(removed))
        first.uiState.first { session ->
            session?.exercises?.none { it.id == "seated-leg-curl" } == true
        }
        val edited = repository.workoutSessions().first().single()
        repository.emitSessions(
            listOf(
                edited.copy(
                    exercises = edited.exercises.map { exercise ->
                        exercise.copy(
                            warmupSets = exercise.warmupSets?.map {
                                it.copy(setState = SetState.Completed())
                            },
                            sets = exercise.sets.map { it.copy(setState = SetState.Completed()) },
                        )
                    },
                    duration = kotlin.time.Duration.ZERO,
                ),
            ),
        )

        val next = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)
        val created = next.uiState.first { session ->
            session != null && session.id != edited.id
        }!!

        assert(created.exercises.map { it.id } == fullBodyA.sets.map { it.exercise.id })
        assert(created.exercises.none { it.id == "incline-db-curl" })
        assert(created.exercises.any { it.id == "seated-leg-curl" })
        assert(repository.workoutPlans().first() == listOf(fullBodyA))
    }
}
