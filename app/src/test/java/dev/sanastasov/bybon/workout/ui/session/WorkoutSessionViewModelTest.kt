package dev.sanastasov.bybon.workout.ui.session

import app.cash.turbine.test
import dev.sanastasov.bybon.test.BackgroundFailures
import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.domain.ExerciseSet
import dev.sanastasov.bybon.workout.domain.ExerciseState
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.catalogExercise
import dev.sanastasov.bybon.workout.domain.catalogExercises
import dev.sanastasov.bybon.workout.domain.formatRestClock
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WorkoutSessionViewModelTest {

    @Test
    fun `creates a session from the plan with planned warmup sets`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            val session = awaitItem()!!
            val bench = session.exercises.first()
            val warmupSets = checkNotNull(bench.warmupSets)
            assert(warmupSets.size == 3)
            assert(
                warmupSets.map { it.weight } == listOf(
                    Weight.kilograms(20),
                    Weight.kilograms(20),
                    Weight.kilograms(20),
                ),
            )
            assert(warmupSets.map { it.reps } == listOf(8, 4, 3))
            assert(bench.sets.size == 3)
            assert(warmupSets.first().setState == SetState.InProgress)
            assert(bench.sets.all { it.setState == SetState.NotStated })
            assert(session.state == WorkoutState.InProgress)
        }
    }

    @Test
    fun `converting set one to warmup persists in the repository`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            val session = awaitItem()!!
            val bench = session.exercises.first()

            viewModel.onAction(WorkoutSessionAction.OnConvertToWarmup(bench))
            val converted = awaitItem()!!
            assert(converted.exercises.first().warmupSets?.size == 4)
            assert(converted.exercises.first().sets.size == 2)
            assert(converted.exercises.first().warmupSets!!.first().setState == SetState.InProgress)

            viewModel.onAction(WorkoutSessionAction.OnConvertToWorkSet(converted.exercises.first()))
            val restored = awaitItem()!!
            assert(restored.exercises.first().warmupSets?.size == 3)
            assert(restored.exercises.first().sets.size == 3)
        }
    }

    @Test
    fun `add set adds a work set and remove last drops the last not completed set`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            val session = awaitItem()!!
            val bench = session.exercises.first()

            viewModel.onAction(WorkoutSessionAction.OnAddSet(bench))
            val added = awaitItem()!!
            assert(added.exercises.first().sets.size == 4)
            assert(added.exercises.first().warmupSets?.size == 3)
            assert(added.exercises.first().sets.last().setState == SetState.NotStated)

            viewModel.onAction(WorkoutSessionAction.RemoveLastSet(added.exercises.first()))
            val removed = awaitItem()!!
            assert(removed.exercises.first().sets.size == 3)
            assert(removed.exercises.first().warmupSets?.size == 3)
        }
    }

    @Test
    fun `completing warmups then work sets walks onto the next exercise warmup`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            var session = awaitItem()!!
            val bench = session.exercises.first()

            checkNotNull(bench.warmupSets).indices.forEach { index ->
                viewModel.onAction(
                    WorkoutSessionAction.OnCompleteSet(
                        session.exercises.first(),
                        index,
                        isWarmup = true,
                    ),
                )
                session = awaitItem()!!
            }
            assert(session.exercises.first().warmupSets!!.all { it.setState == SetState.Completed })
            assert(session.exercises.first().sets.first().setState == SetState.InProgress)

            session.exercises.first().sets.indices.forEach { index ->
                viewModel.onAction(
                    WorkoutSessionAction.OnCompleteSet(session.exercises.first(), index),
                )
                session = awaitItem()!!
            }
            assert(session.exercises.first().sets.all { it.setState == SetState.Completed })
            assert(session.exercises[1].warmupSets!!.first().setState == SetState.InProgress)
        }
    }

    @Test
    fun `session rest timers match compound isolation and default durations`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            val session = awaitItem()!!
            assert(session.exercises.first().restAfterWorkSet == 2.minutes)
            assert(session.exercises.first().restAfterWorkSet.formatRestClock() == "2:00")
            assert(
                session.exercises.first {
                    it.id == "leg-curl"
                }.restAfterWorkSet.formatRestClock() ==
                    "1:30",
            )
            assert(
                session.exercises.first {
                    it.id == "skullcrusher-db"
                }.restAfterWorkSet.formatRestClock() ==
                    "1:00",
            )
        }
    }

    @Test
    fun `resumes an incomplete session instead of replacing it`() = runTest {
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
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialSessions = listOf(paused),
        )
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            val session = awaitItem()!!
            assert(session.startedAt == paused.startedAt)
            assert(session.exercises.first().warmupSets!![0].setState == SetState.Completed)
            assert(session.exercises.first().warmupSets!![1].setState == SetState.InProgress)
            assert(session.state == WorkoutState.InProgress)
        }
    }

    @Test
    fun `canceling a workout deletes it and navigates back`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.effects.test {
            viewModel.uiState.first { it != null }
            viewModel.onAction(WorkoutSessionAction.OnCancelWorkout)
            assert(awaitItem() == WorkoutSessionEffect.NavigateBack)
        }
        assert(repository.workoutSessions().first().isEmpty())
    }

    @Test
    fun `completing the last set completes the workout and opens the summary`() = runTest {
        val bench = catalogExercise("bench-press-bb")
        val startedAt = LocalDateTime.of(2026, 1, 1, 12, 0)
        val session = WorkoutSession(
            planId = fullBodyA.id,
            planName = fullBodyA.name,
            planDescription = null,
            exercises = listOf(
                WorkoutExercise(
                    exerciseDefinition = bench,
                    repRange = 8..10,
                    sets = listOf(
                        ExerciseSet(bench, Weight.kilograms(50), 8, SetState.InProgress),
                    ),
                ),
            ),
            startedAt = startedAt,
        )
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialSessions = listOf(session),
        )
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.effects.test {
            val current = viewModel.uiState.first { it != null }!!
            viewModel.onAction(WorkoutSessionAction.OnCompleteSet(current.exercises.first(), 0))
            assert(awaitItem() == WorkoutSessionEffect.NavigateToSummary(session.id))
        }
        val stored = repository.workoutSessions().first().single()
        assert(stored.state is WorkoutState.Completed)
        assert(stored.exercises.single().state == ExerciseState.Completed)
    }

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
    fun `picked exercise is appended to the session`() = runTest {
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialExercises = catalogExercises,
        )
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            val session = awaitItem()!!
            viewModel.onAction(WorkoutSessionAction.OnExercisePicked("incline-curl-db"))
            val added = awaitItem()!!
            assert(added.exercises.last().id == "incline-curl-db")
            assert(
                added.exercises.last().exerciseDefinition ==
                    catalogExercise("incline-curl-db"),
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
        failures.expectFailure("Exercise bench-press-bb is already in the session") {
            viewModel.onAction(WorkoutSessionAction.OnExercisePicked("bench-press-bb"))
        }
    }

    @Test
    fun `removing an incomplete exercise persists the session without it`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            val session = awaitItem()!!
            val removed = session.exercises.first { it.id == "leg-curl" }

            viewModel.onAction(WorkoutSessionAction.OnRemoveExercise(removed))
            val updated = awaitItem()!!
            assert(updated.exercises.none { it.id == "leg-curl" })
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
            assert(updated.exercises.none { it.id == "bench-press-bb" })
            assert(updated.exercises.first().id == "squat-bb")
            assert(updated.exercises.first().warmupSets!!.first().setState == SetState.InProgress)
        }
    }

    @Test
    fun `removing a completed exercise is rejected`() = runTest {
        val session = twoExerciseSession(
            firstState = SetState.Completed,
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
        failures.expectFailure("Cannot remove completed exercise bench-press-bb") {
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
            exerciseDefinition = catalogExercise("incline-curl-db"),
        )
        failures.expectFailure("Exercise incline-curl-db is not in the session") {
            viewModel.onAction(WorkoutSessionAction.OnRemoveExercise(missing))
        }
    }

    @Test
    fun `removing the last remaining exercise is rejected`() = runTest {
        val bench = catalogExercise("bench-press-bb")
        val session = WorkoutSession(
            planId = fullBodyA.id,
            planName = fullBodyA.name,
            planDescription = null,
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
    fun `removing the last incomplete exercise completes the workout`() = runTest {
        val session = twoExerciseSession(
            firstState = SetState.Completed,
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
            assert(awaitItem() == WorkoutSessionEffect.NavigateToSummary(session.id))
        }
        val stored = repository.workoutSessions().first().single()
        assert(stored.state is WorkoutState.Completed)
        assert(stored.exercises.single().id == "bench-press-bb")
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
        first.onAction(WorkoutSessionAction.OnExercisePicked("incline-curl-db"))
        val added = first.uiState.first { session ->
            session?.exercises?.last()?.id == "incline-curl-db"
        }!!
        val removed = added.exercises.first { it.id == "leg-curl" }
        first.onAction(WorkoutSessionAction.OnRemoveExercise(removed))
        first.uiState.first { session ->
            session?.exercises?.none { it.id == "leg-curl" } == true
        }
        val edited = repository.workoutSessions().first().single()
        repository.emitSessions(
            listOf(
                edited.copy(
                    exercises = edited.exercises.map { exercise ->
                        exercise.copy(
                            warmupSets = exercise.warmupSets?.map {
                                it.copy(setState = SetState.Completed)
                            },
                            sets = exercise.sets.map { it.copy(setState = SetState.Completed) },
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
        assert(created.exercises.none { it.id == "incline-curl-db" })
        assert(created.exercises.any { it.id == "leg-curl" })
        assert(repository.workoutPlans().first() == listOf(fullBodyA))
    }
}

private fun twoExerciseSession(firstState: SetState, secondState: SetState): WorkoutSession {
    val bench = catalogExercise("bench-press-bb")
    val curl = catalogExercise("incline-curl-db")
    return WorkoutSession(
        planId = fullBodyA.id,
        planName = fullBodyA.name,
        planDescription = null,
        exercises = listOf(
            WorkoutExercise(
                exerciseDefinition = bench,
                repRange = 8..10,
                sets = listOf(ExerciseSet(bench, Weight.kilograms(50), 8, firstState)),
            ),
            WorkoutExercise(
                exerciseDefinition = curl,
                repRange = 8..12,
                sets = listOf(ExerciseSet(curl, Weight.kilograms(12), 8, secondState)),
            ),
        ),
        startedAt = LocalDateTime.of(2026, 1, 1, 12, 0),
    )
}
