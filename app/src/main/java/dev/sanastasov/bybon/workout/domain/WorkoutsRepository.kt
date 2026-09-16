package dev.sanastasov.bybon.workout.domain

import kotlinx.coroutines.flow.Flow

interface WorkoutsRepository {

    fun exercises(): Flow<List<ExerciseDefinition>>

    fun workoutPlans(
        filter: WorkoutPlansFilter = WorkoutPlansFilter.UnarchivedPlans,
    ): Flow<List<WorkoutPlan>>

    suspend fun archivePlan(planId: WorkoutPlanId, archived: Boolean)

    suspend fun updatePlan(planId: WorkoutPlanId, transform: (WorkoutPlan) -> WorkoutPlan)

    suspend fun updateWorkout(session: WorkoutSession)

    fun workoutSessions(): Flow<List<WorkoutSession>>

    suspend fun importHistory(
        plans: List<WorkoutPlan>,
        sessions: List<WorkoutSession>,
        exercises: List<ExerciseDefinition>,
    )
}
