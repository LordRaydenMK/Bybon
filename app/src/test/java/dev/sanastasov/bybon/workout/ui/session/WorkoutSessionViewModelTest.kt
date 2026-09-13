package dev.sanastasov.bybon.workout.ui.session

import app.cash.turbine.test
import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutSessionAction
import dev.sanastasov.bybon.workout.domain.fullBodyA
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
            assert(bench.warmupSets.size == 3)
            assert(
                bench.warmupSets.map { it.weight } == listOf(
                Weight.kilograms(20),
                Weight.kilograms(20),
                Weight.kilograms(20),
            )
            )
            assert(bench.warmupSets.map { it.reps } == listOf(8, 4, 3))
            assert(bench.sets.size == 3)
            assert(bench.warmupSets.first().setState == SetState.InProgress)
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
            assert(converted.exercises.first().warmupSets.size == 4)
            assert(converted.exercises.first().sets.size == 2)
            assert(converted.exercises.first().warmupSets.first().setState == SetState.InProgress)

            viewModel.onAction(WorkoutSessionAction.OnConvertToWorkSet(converted.exercises.first()))
            val restored = awaitItem()!!
            assert(restored.exercises.first().warmupSets.size == 3)
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
            assert(added.exercises.first().warmupSets.size == 3)
            assert(added.exercises.first().sets.last().setState == SetState.NotStated)

            viewModel.onAction(WorkoutSessionAction.RemoveLastSet(added.exercises.first()))
            val removed = awaitItem()!!
            assert(removed.exercises.first().sets.size == 3)
            assert(removed.exercises.first().warmupSets.size == 3)
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

            bench.warmupSets.indices.forEach { index ->
                viewModel.onAction(
                    WorkoutSessionAction.OnCompleteSet(
                        session.exercises.first(),
                        index,
                        isWarmup = true
                    ),
                )
                session = awaitItem()!!
            }
            assert(session.exercises.first().warmupSets.all { it.setState == SetState.Completed })
            assert(session.exercises.first().sets.first().setState == SetState.InProgress)

            session.exercises.first().sets.indices.forEach { index ->
                viewModel.onAction(
                    WorkoutSessionAction.OnCompleteSet(session.exercises.first(), index),
                )
                session = awaitItem()!!
            }
            assert(session.exercises.first().sets.all { it.setState == SetState.Completed })
            assert(session.exercises[1].warmupSets.first().setState == SetState.InProgress)
        }
    }
}
