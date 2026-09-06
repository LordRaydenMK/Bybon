package dev.sanastasov.bybon.workout.domain

import kotlinx.coroutines.flow.Flow

interface WorkoutPlansRepository {

    fun workoutPlans(): Flow<List<WorkoutPlan>>
}