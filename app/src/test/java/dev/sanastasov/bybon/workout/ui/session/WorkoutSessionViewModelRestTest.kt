package dev.sanastasov.bybon.workout.ui.session

import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.completeSet
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WorkoutSessionViewModelRestTest {

    @Test
    fun `startWorkout in init is a no-op while rest remaining is greater than zero`() = runTest {
        val completedAt = LocalDateTime.now()
        val startedAt = completedAt.minusMinutes(4)
        val session = fullBodyA.toWorkoutSession(startedAt = startedAt)
        val warmupCount = session.exercises.first().warmupSets?.size ?: 0
        val resting = (0 until warmupCount).fold(session) { current, index ->
            current.completeSet(
                current.exercises.first(),
                index,
                isWarmup = true,
                now = completedAt,
            )
        }
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialSessions = listOf(resting),
        )
        val viewModel = WorkoutSessionViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.first { it != null }
        val stored = repository.workoutSessions().first().single()
        assert(stored.state is WorkoutState.Resting)
        assert(stored.exercises.first().sets.first().setState == SetState.NotStated)
    }
}
