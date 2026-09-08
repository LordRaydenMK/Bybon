package dev.sanastasov.bybon.workout.data

import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class WorkoutsRepositoryImpl : WorkoutsRepository {

    private val workoutPlan = MutableStateFlow<WorkoutSession?>(null)

    override fun workoutPlans(): Flow<List<WorkoutPlan>> = flowOf(listOf(fullBodyA, fullBodyB))

    override suspend fun updateWorkout(session: WorkoutSession) {
        workoutPlan.update { session }
    }

    override fun workoutSessions(): Flow<List<WorkoutSession>> = workoutPlan.map {
        if (it == null) emptyList()
        else listOf(it)
    }
}