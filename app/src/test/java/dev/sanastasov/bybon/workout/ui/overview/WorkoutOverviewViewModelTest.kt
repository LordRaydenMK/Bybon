package dev.sanastasov.bybon.workout.ui.overview

import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import dev.sanastasov.bybon.workout.domain.fullBodyA
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Test

class WorkoutOverviewViewModelTest {

    @Test
    fun `adjustments stay in memory until start persists the session`() = runBlocking {
        val repository = RecordingWorkoutsRepository()
        val viewModel = WorkoutOverviewViewModel(fullBodyA.id, repository, this)

        val draft = viewModel.uiState.filterNotNull().first()
        assert(repository.saved.isEmpty())
        assert(draft.workoutSets.none { it.setState == SetState.InProgress })

        viewModel.onAction(WorkoutOverviewAction.OnIncreaseWorkout)
        assert(repository.saved.isEmpty())
        val increased = viewModel.uiState.filterNotNull().first()
        assert(increased.exercises.first().sets.first().reps == draft.exercises.first().sets.first().reps + 1)

        viewModel.onAction(WorkoutOverviewAction.OnStartWorkout)
        assert(viewModel.effects.first() == WorkoutOverviewEffect.NavigateToSession)
        assert(repository.saved.size == 1)
        assert(repository.saved.single().exercises.first().sets.first().setState == SetState.InProgress)
        assert(repository.saved.single().exercises.first().sets.first().reps == increased.exercises.first().sets.first().reps)
    }
}

private class RecordingWorkoutsRepository(
    plans: List<WorkoutPlan> = listOf(fullBodyA),
    sessions: List<WorkoutSession> = emptyList(),
) : WorkoutsRepository {
    private val plansFlow = MutableStateFlow(plans)
    private val sessionsFlow = MutableStateFlow(sessions)
    val saved = mutableListOf<WorkoutSession>()

    override fun workoutPlans(): Flow<List<WorkoutPlan>> = plansFlow

    override fun workoutSessions(): Flow<List<WorkoutSession>> = sessionsFlow

    override suspend fun updateWorkout(session: WorkoutSession) {
        saved += session
        sessionsFlow.update { list ->
            val index = list.indexOfFirst {
                it.planId == session.planId && it.state !is WorkoutState.Completed
            }
            if (index >= 0) {
                list.toMutableList().apply { set(index, session) }
            } else {
                list + session
            }
        }
    }
}
