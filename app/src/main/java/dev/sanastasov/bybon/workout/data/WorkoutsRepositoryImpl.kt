package dev.sanastasov.bybon.workout.data

import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update


class WorkoutsRepositoryImpl : WorkoutsRepository {

    private val sessions = MutableStateFlow(listOf(sampleFullBodyBCompleted))

    override fun workoutPlans(): Flow<List<WorkoutPlan>> = flowOf(listOf(fullBodyA, fullBodyB))

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
}
