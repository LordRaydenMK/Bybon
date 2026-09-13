package dev.sanastasov.bybon.workout.domain

import kotlinx.coroutines.flow.Flow

interface WorkoutsRepository {

    fun workoutPlans(): Flow<List<WorkoutPlan>>

    suspend fun updateWorkout(session: WorkoutSession)

    fun workoutSessions(): Flow<List<WorkoutSession>>

    suspend fun importHistory(plans: List<WorkoutPlan>, sessions: List<WorkoutSession>)
}
