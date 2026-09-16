package dev.sanastasov.bybon.workout.domain

import kotlinx.coroutines.flow.Flow

interface WorkoutsRepository {

    fun exercises(): Flow<List<ExerciseDefinition>>

    fun workoutPlans(
        filter: WorkoutPlansFilter = WorkoutPlansFilter.ActivePlans,
    ): Flow<List<WorkoutPlan>>

    suspend fun archivePlan(planId: WorkoutPlanId, archived: Boolean)

    suspend fun updateWorkout(session: WorkoutSession)

    fun workoutSessions(): Flow<List<WorkoutSession>>

    suspend fun importHistory(
        plans: List<WorkoutPlan>,
        sessions: List<WorkoutSession>,
        exercises: List<ExerciseDefinition>,
    )
}
