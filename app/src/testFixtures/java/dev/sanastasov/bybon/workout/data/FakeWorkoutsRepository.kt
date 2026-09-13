package dev.sanastasov.bybon.workout.data

import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeWorkoutsRepository(
    initialPlans: List<WorkoutPlan> = emptyList(),
    initialSessions: List<WorkoutSession> = emptyList(),
) : WorkoutsRepository {

    private val plans = MutableStateFlow(initialPlans)
    private val sessions = MutableStateFlow(initialSessions)

    override fun workoutPlans(): Flow<List<WorkoutPlan>> = plans

    override suspend fun updateWorkout(session: WorkoutSession) {
        sessions.update { it + session }
    }

    override fun workoutSessions(): Flow<List<WorkoutSession>> = sessions

    fun emitSessions(value: List<WorkoutSession>) {
        sessions.value = value
    }
}
