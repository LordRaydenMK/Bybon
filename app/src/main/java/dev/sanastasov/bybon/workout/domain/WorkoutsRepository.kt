package dev.sanastasov.bybon.workout.domain

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface WorkoutsRepository {

    fun exercises(): Flow<List<ExerciseDefinition>>

    fun workoutPlans(
        filter: WorkoutPlansFilter = WorkoutPlansFilter.UnarchivedPlans,
    ): Flow<List<WorkoutPlan>>

    suspend fun archivePlan(planId: WorkoutPlanId, archived: Boolean)

    suspend fun updatePlan(planId: WorkoutPlanId, transform: (WorkoutPlan) -> WorkoutPlan)

    suspend fun updateWorkout(session: WorkoutSession)

    suspend fun deleteWorkout(sessionId: WorkoutSessionId)

    fun workoutSessions(): Flow<List<WorkoutSession>>

    fun workoutSession(planId: WorkoutPlanId): Flow<List<WorkoutSession>>

    suspend fun sevenDayBodyWeight(on: LocalDate): Weight?

    suspend fun importHistory(
        plans: List<WorkoutPlan>,
        sessions: List<WorkoutSession>,
        exercises: List<ExerciseDefinition>,
    ): ImportHistoryResult
}

data class ImportHistoryResult(
    val plans: List<WorkoutPlan>,
    val sessions: List<WorkoutSession>,
    val exercises: List<ExerciseDefinition>,
    val sessionsSkipped: Int,
)
