package dev.sanastasov.bybon.workout.data

import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class WorkoutsRepositoryImpl : WorkoutsRepository {

    private val plans = MutableStateFlow(listOf(fullBodyA, fullBodyB))
    private val sessions = MutableStateFlow<List<WorkoutSession>>(emptyList())

    override fun workoutPlans(): Flow<List<WorkoutPlan>> = plans

    override suspend fun updateWorkout(session: WorkoutSession) {
        sessions.update { list ->
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

    override fun workoutSessions(): Flow<List<WorkoutSession>> = sessions

    override suspend fun importHistory(
        plans: List<WorkoutPlan>,
        sessions: List<WorkoutSession>,
    ) {
        this.plans.update { it + plans }
        this.sessions.update { it + sessions }
    }
}
