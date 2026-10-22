@file:Suppress("TooManyFunctions")

package dev.sanastasov.bybon.workout.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {

    @Transaction
    @Query("SELECT * FROM workout_session ORDER BY rowid")
    fun observeSessionTrees(): Flow<List<WorkoutSessionTree>>

    @Transaction
    @Query("SELECT * FROM workout_session WHERE id = :id")
    suspend fun sessionTree(id: Long): WorkoutSessionTree?

    @Query("SELECT * FROM workout_session ORDER BY rowid")
    fun observeSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_session_exercise ORDER BY rowid")
    fun observeSessionExercises(): Flow<List<WorkoutSessionExerciseEntity>>

    @Query("SELECT * FROM workout_set ORDER BY rowid")
    fun observeSets(): Flow<List<WorkoutSetEntity>>

    @Query("SELECT plan_id, started_at FROM workout_session")
    suspend fun sessionIdentities(): List<WorkoutSessionIdentity>

    @Query(
        "SELECT * FROM workout_session WHERE plan_id = :planId AND duration_seconds IS NULL LIMIT 1",
    )
    suspend fun openSessionForPlan(planId: String): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_session_exercise WHERE session_id = :sessionId ORDER BY position")
    suspend fun exercisesForSession(sessionId: Long): List<WorkoutSessionExerciseEntity>

    @Query(
        "SELECT * FROM workout_set WHERE session_exercise_id IN (:sessionExerciseIds) ORDER BY position",
    )
    suspend fun setsForExercises(sessionExerciseIds: List<Long>): List<WorkoutSetEntity>

    @Insert
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Insert
    suspend fun insertSessionExercise(exercise: WorkoutSessionExerciseEntity): Long

    @Insert
    suspend fun insertSets(sets: List<WorkoutSetEntity>)

    @Query(
        """
        UPDATE workout_session
        SET plan_name = :planName, note = :note, duration_seconds = :durationSeconds
        WHERE id = :id
        """,
    )
    suspend fun updateSessionHeader(
        id: Long,
        planName: String,
        note: String?,
        durationSeconds: Int?,
    )

    @Query("DELETE FROM workout_session_exercise WHERE session_id = :sessionId")
    suspend fun deleteExercisesForSession(sessionId: Long)

    @Query("DELETE FROM workout_session WHERE plan_id = :planId AND started_at = :startedAt")
    suspend fun deleteSession(planId: String, startedAt: LocalDateTime)

    @Insert
    suspend fun insertImportedExercises(exercises: List<ExerciseEntity>)

    @Insert
    suspend fun insertImportedPlans(plans: List<WorkoutPlanEntity>)

    @Insert
    suspend fun insertImportedPlanExercises(exercises: List<WorkoutPlanExerciseEntity>)

    @Transaction
    suspend fun insertSessionTree(write: WorkoutSessionWrite): Long {
        val sessionId = insertSession(write.header)
        insertChildren(sessionId, write.exercises)
        return sessionId
    }

    @Transaction
    suspend fun replaceSessionTree(sessionId: Long, write: WorkoutSessionWrite) {
        updateSessionHeader(
            id = sessionId,
            planName = write.header.planName,
            note = write.header.note,
            durationSeconds = write.header.durationSeconds,
        )
        deleteExercisesForSession(sessionId)
        insertChildren(sessionId, write.exercises)
    }

    @Transaction
    suspend fun importAdded(
        exercises: List<ExerciseEntity>,
        plans: List<WorkoutPlanEntity>,
        planExercises: List<WorkoutPlanExerciseEntity>,
        sessions: List<WorkoutSessionWrite>,
    ) {
        if (exercises.isNotEmpty()) insertImportedExercises(exercises)
        if (plans.isNotEmpty()) {
            insertImportedPlans(plans)
            insertImportedPlanExercises(planExercises)
        }
        sessions.forEach { insertSessionTree(it) }
    }

    private suspend fun insertChildren(
        sessionId: Long,
        exercises: List<WorkoutSessionExerciseWrite>,
    ) {
        require(exercises.isNotEmpty()) { "Session must contain at least one exercise" }
        exercises.forEach { exercise ->
            val exerciseId = insertSessionExercise(exercise.entity.copy(sessionId = sessionId))
            val sets = exercise.sets.map { set -> set.copy(sessionExerciseId = exerciseId) }
            require(sets.isNotEmpty()) { "Session exercise must contain at least one set" }
            insertSets(sets)
        }
    }
}
