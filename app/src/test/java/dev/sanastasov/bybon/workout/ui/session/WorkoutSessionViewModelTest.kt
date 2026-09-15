package dev.sanastasov.bybon.workout.ui.session

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.formatRestClock
import dev.sanastasov.bybon.workout.domain.fullBodyA
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
        assert(viewModel.effects.first() == WorkoutSessionEffect.ShowExercise(1))
    }

    @Test
    fun `uncomplete last work set restores it and demotes the next exercise`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            var session = awaitItem()!!
            session = completeFirstExercise(viewModel, session)
            assert(session.exercises.first().sets.all { it.setState == SetState.Completed })
            assert(session.exercises[1].warmupSets!!.first().setState == SetState.InProgress)

            viewModel.onAction(
                WorkoutSessionAction.OnUncompleteSet(session.exercises.first(), 2),
            )
            session = awaitItem()!!
            assert(session.exercises.first().sets.last().setState == SetState.InProgress)
            assert(session.exercises[1].warmupSets!!.first().setState == SetState.NotStated)
        }
    }

    @Test
    fun `adding a set to a completed exercise starts it and resets the next exercise`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            var session = awaitItem()!!
            session = completeFirstExercise(viewModel, session)

            viewModel.onAction(WorkoutSessionAction.OnAddSet(session.exercises.first()))
            session = awaitItem()!!
            assert(session.exercises.first().sets.size == 4)
            assert(session.exercises.first().sets.last().setState == SetState.InProgress)
            assert(session.exercises[1].warmupSets!!.first().setState == SetState.NotStated)
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
}

private suspend fun ReceiveTurbine<WorkoutSession?>.completeFirstExercise(
    viewModel: WorkoutSessionViewModel,
    initial: WorkoutSession,
): WorkoutSession {
    var session = initial
    checkNotNull(session.exercises.first().warmupSets).indices.forEach { index ->
        viewModel.onAction(
            WorkoutSessionAction.OnCompleteSet(
                session.exercises.first(),
                index,
                isWarmup = true,
            ),
        )
        session = awaitItem()!!
    }
    session.exercises.first().sets.indices.forEach { index ->
        viewModel.onAction(WorkoutSessionAction.OnCompleteSet(session.exercises.first(), index))
        session = awaitItem()!!
    }
    return session
}
