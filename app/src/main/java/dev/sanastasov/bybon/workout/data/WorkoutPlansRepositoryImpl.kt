package dev.sanastasov.bybon.workout.data

import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.WorkoutPlansRepository
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class WorkoutPlansRepositoryImpl : WorkoutPlansRepository {
    override fun workoutPlans(): Flow<List<WorkoutPlan>> = flowOf(listOf(fullBodyA, fullBodyB))
}