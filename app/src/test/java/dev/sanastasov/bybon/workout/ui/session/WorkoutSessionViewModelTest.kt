package dev.sanastasov.bybon.workout.ui.session

import app.cash.turbine.test
import dev.sanastasov.bybon.workout.REST_PROMOTE_LEAD
import dev.sanastasov.bybon.workout.RestPromoteScheduler
import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.data.completedExercise
import dev.sanastasov.bybon.workout.data.completedSession
import dev.sanastasov.bybon.workout.domain.ExerciseSet
import dev.sanastasov.bybon.workout.domain.ExerciseState
import dev.sanastasov.bybon.workout.domain.PreviousSetPerformance
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.catalogExercise
import dev.sanastasov.bybon.workout.domain.completeInProgressSet
import dev.sanastasov.bybon.workout.domain.formatRestClock
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.restTimer
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import dev.sanastasov.bybon.workout.domain.updateExercise
import java.time.LocalDateTime
import kotlin.time.Duration
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
    fun `session work previous stays aligned after dropping a warmup`() = runTest {
        val previous = completedSessionWithDistinctSets()
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialSessions = listOf(previous),
        )
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            val session = awaitItem()!!
            val bench = session.exercises.first()
            assert(bench.warmupSets?.size == 3)
            assert(bench.sets.size == 3)

            viewModel.onAction(WorkoutSessionAction.OnConvertToWorkSet(bench))
            val converted = awaitItem()!!
            viewModel.onAction(WorkoutSessionAction.RemoveLastSet(converted.exercises.first()))
            val reduced = awaitItem()!!
            val updated = reduced.exercises.first()

            assert(updated.warmupSets?.size == 2)
            assert(updated.sets.size == 3)
            assert(
                updated.sets.map { it.previous } == listOf(
                    PreviousSetPerformance(Weight.kilograms(80), 8),
                    PreviousSetPerformance(Weight.kilograms(81), 9),
                    PreviousSetPerformance(Weight.kilograms(82), 10),
                ),
            )
        }
    }

    @Test
    fun `added work set picks up the matching previous work set`() = runTest {
        val previous = completedSessionWithDistinctSets().let { session ->
            session.updateExercise(session.exercises.first().id) { bench ->
                bench.copy(
                    sets = bench.sets + ExerciseSet(
                        bench.exerciseDefinition,
                        Weight.kilograms(83),
                        11,
                        SetState.Completed(),
                    ),
                )
            }
        }
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialSessions = listOf(previous),
        )
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            val session = awaitItem()!!
            viewModel.onAction(WorkoutSessionAction.OnAddSet(session.exercises.first()))
            val added = awaitItem()!!
            val bench = added.exercises.first()

            assert(bench.sets.size == 4)
            assert(
                bench.sets.last().previous == PreviousSetPerformance(Weight.kilograms(83), 11),
            )
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
            assert(session.exercises.first().warmupSets!!.all { it.setState is SetState.Completed })
            assert(session.exercises.first().sets.first().setState == SetState.InProgress)

            session.exercises.first().sets.indices.forEach { index ->
                viewModel.onAction(
                    WorkoutSessionAction.OnCompleteSet(session.exercises.first(), index),
                )
                session = awaitItem()!!
            }
            assert(session.exercises.first().sets.all { it.setState is SetState.Completed })
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
                    it.id == "seated-leg-curl"
                }.restAfterWorkSet.formatRestClock() ==
                    "1:00",
            )
            assert(
                session.exercises.first {
                    it.id == "db-skull-crusher"
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
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialSessions = listOf(paused),
        )
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            val session = awaitItem()!!
            assert(session.startedAt == paused.startedAt)
            assert(session.exercises.first().warmupSets!![0].setState is SetState.Completed)
            assert(session.exercises.first().warmupSets!![1].setState == SetState.InProgress)
            assert(session.state == WorkoutState.InProgress)
        }
    }

    @Test
    fun `canceling a workout deletes it and navigates back`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val scheduler = FakeRestPromoteScheduler()
        val viewModel = WorkoutSessionViewModel(
            fullBodyA.id,
            repository,
            backgroundScope,
            scheduler,
        )

        viewModel.effects.test {
            viewModel.uiState.first { it != null }
            viewModel.onAction(WorkoutSessionAction.OnCancelWorkout)
            assert(awaitItem() == WorkoutSessionEffect.StopService)
            assert(awaitItem() == WorkoutSessionEffect.NavigateBack)
        }
        assert(repository.workoutSessions().first().isEmpty())
        assert(scheduler.state == FakeRestPromoteScheduler.State.Cancelled)
    }

    @Test
    fun `completing the last set completes the workout and opens the summary`() = runTest {
        val bench = catalogExercise("bench-press")
        val startedAt = LocalDateTime.of(2026, 1, 1, 12, 0)
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
            startedAt = startedAt,
        )
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialSessions = listOf(session),
        )
        val scheduler = FakeRestPromoteScheduler()
        val viewModel = WorkoutSessionViewModel(
            fullBodyA.id,
            repository,
            backgroundScope,
            scheduler,
        )

        viewModel.effects.test {
            val current = viewModel.uiState.first { it != null }!!
            viewModel.onAction(WorkoutSessionAction.OnCompleteSet(current.exercises.first(), 0))
            assert(awaitItem() == WorkoutSessionEffect.StopService)
            assert(awaitItem() == WorkoutSessionEffect.NavigateToSummary(session.id))
            expectNoEvents()
        }
        assert(scheduler.state == FakeRestPromoteScheduler.State.Cancelled)
        val stored = repository.workoutSessions().first().single()
        assert(stored.state is WorkoutState.Completed)
        assert(stored.exercises.single().state == ExerciseState.Completed)
    }

    @Test
    fun `completing the last set from the repository opens the summary`() = runTest {
        val bench = catalogExercise("bench-press")
        val startedAt = LocalDateTime.of(2026, 1, 1, 12, 0)
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
            startedAt = startedAt,
        )
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialSessions = listOf(session),
        )
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.effects.test {
            viewModel.uiState.first { it != null }
            repository.completeInProgressSet()
            assert(awaitItem() == WorkoutSessionEffect.StopService)
            assert(awaitItem() == WorkoutSessionEffect.NavigateToSummary(session.id))
            expectNoEvents()
        }
    }

    @Test
    fun `completing an exercise scrolls to the next page`() = runTest {
        val session = twoExerciseSession(
            firstState = SetState.InProgress,
            secondState = SetState.NotStated,
        )
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialSessions = listOf(session),
        )
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.effects.test {
            val current = viewModel.uiState.first { it != null }!!
            viewModel.onAction(WorkoutSessionAction.OnCompleteSet(current.exercises.first(), 0))
            assert(awaitItem() == WorkoutSessionEffect.ScrollToExercise(1))
            expectNoEvents()
        }
        val stored = repository.workoutSessions().first().single()
        assert(stored.exercises.first().state == ExerciseState.Completed)
        assert(stored.exercises[1].sets.first().setState == SetState.InProgress)
    }

    @Test
    fun `completing a warmup does not scroll to the next exercise`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.effects.test {
            val session = viewModel.uiState.first { it != null }!!
            viewModel.onAction(
                WorkoutSessionAction.OnCompleteSet(
                    session.exercises.first(),
                    0,
                    isWarmup = true,
                ),
            )
            viewModel.uiState.first { current ->
                current?.exercises?.first()?.warmupSets?.first()?.setState is SetState.Completed
            }
            expectNoEvents()
        }
    }

    @Test
    fun `completing a non-last work set does not scroll to the next exercise`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.effects.test {
            var session = viewModel.uiState.first { it != null }!!
            checkNotNull(session.exercises.first().warmupSets).indices.forEach { index ->
                viewModel.onAction(
                    WorkoutSessionAction.OnCompleteSet(
                        session.exercises.first(),
                        index,
                        isWarmup = true,
                    ),
                )
                session = viewModel.uiState.first { current ->
                    val setState = current?.exercises?.first()?.warmupSets?.get(index)?.setState
                    setState is SetState.Completed
                }!!
            }
            viewModel.onAction(WorkoutSessionAction.OnCompleteSet(session.exercises.first(), 0))
            viewModel.uiState.first { current ->
                current?.exercises?.first()?.sets?.first()?.setState is SetState.Completed
            }
            expectNoEvents()
        }
    }

    @Test
    fun `keeps the live session when a snapshot omits it`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            val session = awaitItem()!!
            repository.emitSessions(emptyList())
            expectNoEvents()
            repository.emitSessions(
                listOf(
                    completedSession(
                        planId = fullBodyA.id.id,
                        planName = fullBodyA.name,
                        startedAt = LocalDateTime.of(2026, 1, 1, 12, 0),
                        exercises = listOf(completedExercise("bench-press", 50f to 8)),
                    ),
                ),
            )
            expectNoEvents()
            repository.emitSessions(listOf(session.copy(note = "updated")))
            assert(awaitItem()!!.note == "updated")
        }
    }

    @Test
    fun `clears uiState when the same session is completed`() = runTest {
        val live = twoExerciseSession(SetState.InProgress, SetState.NotStated)
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialSessions = listOf(live),
        )
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            assert(awaitItem()!!.id == live.id)
            repository.emitSessions(
                listOf(
                    completedSession(
                        planId = live.planId.id,
                        planName = live.planName,
                        startedAt = live.startedAt,
                        exercises = listOf(completedExercise("bench-press", 50f to 8)),
                    ),
                ),
            )
            assert(awaitItem() == null)
        }
    }
}

class LiveSessionOrLastTest {

    @Test
    fun `prefers the live session in the snapshot`() {
        val live = fullBodyA.toWorkoutSession()
        val previous = live.copy(note = "stale")
        assert(liveSessionOrLast(fullBodyA.id, previous, listOf(live)) == live)
    }

    @Test
    fun `keeps the last live session when it is omitted`() {
        val live = fullBodyA.toWorkoutSession()
        assert(liveSessionOrLast(fullBodyA.id, live, emptyList()) == live)
        assert(
            liveSessionOrLast(
                fullBodyA.id,
                live,
                listOf(
                    completedSession(
                        planId = fullBodyA.id.id,
                        planName = fullBodyA.name,
                        startedAt = LocalDateTime.of(2025, 1, 1, 12, 0),
                        exercises = listOf(completedExercise("bench-press", 50f to 8)),
                    ),
                ),
            ) == live,
        )
    }

    @Test
    fun `clears when the same session id is completed`() {
        val live = fullBodyA.toWorkoutSession(
            startedAt = LocalDateTime.of(2026, 1, 1, 12, 0),
        )
        val done = completedSession(
            planId = live.planId.id,
            planName = live.planName,
            startedAt = live.startedAt,
            exercises = listOf(completedExercise("bench-press", 50f to 8)),
        )
        assert(liveSessionOrLast(fullBodyA.id, live, listOf(done)) == null)
    }

    @Test
    fun `completing a rest-starting set schedules rest promote 3s early`() = runTest {
        val scheduler = FakeRestPromoteScheduler()
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutSessionViewModel(
            fullBodyA.id,
            repository,
            backgroundScope,
            scheduler,
        )

        viewModel.uiState.test {
            skipItems(1)
            var session = awaitItem()!!
            val warmupCount = session.exercises.first().warmupSets!!.size
            repeat(warmupCount) { index ->
                viewModel.onAction(
                    WorkoutSessionAction.OnCompleteSet(
                        session.exercises.first(),
                        index,
                        isWarmup = true,
                    ),
                )
                session = awaitItem()!!
            }
            val rest = checkNotNull(session.restTimer())
            assert(
                scheduler.state == FakeRestPromoteScheduler.State.Scheduled(
                    rest.duration - REST_PROMOTE_LEAD,
                ),
            )
        }
    }
}

private class FakeRestPromoteScheduler : RestPromoteScheduler {
    sealed interface State {
        data object Idle : State
        data class Scheduled(
            val wait: Duration,
        ) : State
        data object Cancelled : State
    }

    var state: State = State.Idle
        private set

    override fun schedule(wait: Duration) {
        state = State.Scheduled(wait)
    }

    override fun cancel() {
        state = State.Cancelled
    }
}

private fun completedSessionWithDistinctSets(): WorkoutSession =
    fullBodyA.toWorkoutSession().let { session ->
        session.copy(
            exercises = session.exercises.map { exercise ->
                exercise.copy(
                    sets = exercise.sets.mapIndexed { index, set ->
                        set.copy(
                            weight = Weight.kilograms(80 + index),
                            reps = 8 + index,
                            setState = SetState.Completed(),
                        )
                    },
                    warmupSets = exercise.warmupSets?.mapIndexed { index, set ->
                        set.copy(
                            weight = Weight.kilograms(20 + index),
                            reps = 8,
                            setState = SetState.Completed(),
                        )
                    },
                )
            },
            duration = kotlin.time.Duration.ZERO,
            startedAt = LocalDateTime.of(2026, 10, 1, 12, 0),
        )
    }

internal fun twoExerciseSession(firstState: SetState, secondState: SetState): WorkoutSession {
    val bench = catalogExercise("bench-press")
    val curl = catalogExercise("incline-db-curl")
    return WorkoutSession(
        planId = fullBodyA.id,
        planName = fullBodyA.name,
        note = null,
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
