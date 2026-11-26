@file:Suppress("TooManyFunctions")

package dev.sanastasov.bybon.workout.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutPlanDao {

    @Query("SELECT * FROM workout_plan ORDER BY position")
    fun observePlans(): Flow<List<WorkoutPlanEntity>>

    @Query("SELECT * FROM workout_plan_exercise ORDER BY rowid")
    fun observePlanExercises(): Flow<List<WorkoutPlanExerciseEntity>>

    @Query("SELECT id FROM workout_plan")
    suspend fun ids(): List<String>

    @Query("SELECT * FROM workout_plan WHERE id = :planId")
    suspend fun planById(planId: String): WorkoutPlanEntity?

    @Query("SELECT * FROM workout_plan_exercise WHERE plan_id = :planId ORDER BY position")
    suspend fun exercisesForPlan(planId: String): List<WorkoutPlanExerciseEntity>

    @Query("UPDATE workout_plan SET is_archived = :archived WHERE id = :planId")
    suspend fun setArchived(planId: String, archived: Boolean)

    @Query("SELECT COALESCE(MAX(position), -1) FROM workout_plan")
    suspend fun maxPosition(): Int

    @Query("UPDATE workout_plan SET position = :position WHERE id = :planId")
    suspend fun setPosition(planId: String, position: Int)

    @Transaction
    suspend fun setPositions(idsInOrder: List<String>) {
        idsInOrder.forEachIndexed { index, id ->
            setPosition(id, index)
        }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlans(plans: List<WorkoutPlanEntity>)

    @Insert
    suspend fun insertPlanExercises(exercises: List<WorkoutPlanExerciseEntity>)

    @Upsert
    suspend fun upsertPlan(plan: WorkoutPlanEntity)

    @Query("DELETE FROM workout_plan_exercise WHERE plan_id = :planId")
    suspend fun deleteExercisesForPlan(planId: String)

    @Transaction
    suspend fun replacePlan(plan: WorkoutPlanEntity, exercises: List<WorkoutPlanExerciseEntity>) {
        require(exercises.isNotEmpty()) { "Plan ${plan.id} must contain at least one exercise" }
        upsertPlan(plan)
        deleteExercisesForPlan(plan.id)
        insertPlanExercises(exercises)
    }

    @Transaction
    suspend fun insertPlansWithExercises(
        plans: List<WorkoutPlanEntity>,
        exercises: List<WorkoutPlanExerciseEntity>,
    ) {
        require(plans.isNotEmpty()) { "Use insertPlanExercises to insert exercises only" }
        require(exercises.isNotEmpty()) { "Use insertPlans to insert plans only" }
        insertPlans(plans)
        insertPlanExercises(exercises)
    }
}
