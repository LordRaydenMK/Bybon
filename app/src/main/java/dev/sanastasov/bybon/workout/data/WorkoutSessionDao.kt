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

    @Query(
        """
        SELECT * FROM workout_session
        WHERE (:planId IS NULL OR plan_id = :planId)
          AND (
            :state IS NULL
            OR (
              :state = 'Draft'
              AND NOT EXISTS (
                SELECT 1 FROM workout_set s
                JOIN workout_session_exercise e ON e.id = s.session_exercise_id
                WHERE e.session_id = workout_session.id
                  AND s.set_state != 'NotStated'
              )
            )
            OR (
              :state = 'InProgress'
              AND EXISTS (
                SELECT 1 FROM workout_set s
                JOIN workout_session_exercise e ON e.id = s.session_exercise_id
                WHERE e.session_id = workout_session.id
                  AND s.set_state != 'NotStated'
              )
              AND EXISTS (
                SELECT 1 FROM workout_set s
                JOIN workout_session_exercise e ON e.id = s.session_exercise_id
                WHERE e.session_id = workout_session.id
                  AND s.set_state != 'Completed'
              )
            )
            OR (
              :state = 'Completed'
              AND EXISTS (
                SELECT 1 FROM workout_set s
                JOIN workout_session_exercise e ON e.id = s.session_exercise_id
                WHERE e.session_id = workout_session.id
              )
              AND NOT EXISTS (
                SELECT 1 FROM workout_set s
                JOIN workout_session_exercise e ON e.id = s.session_exercise_id
                WHERE e.session_id = workout_session.id
                  AND s.set_state != 'Completed'
              )
            )
          )
        ORDER BY rowid
        """,
    )
    fun observeSessions(
        planId: String? = null,
        state: String? = null,
    ): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_session_exercise ORDER BY rowid")
    fun observeSessionExercises(): Flow<List<WorkoutSessionExerciseEntity>>

    @Query("SELECT * FROM workout_set ORDER BY rowid")
    fun observeSets(): Flow<List<WorkoutSetEntity>>

    @Query(
        """
        SELECT * FROM workout_session
        WHERE duration_seconds IS NULL
          AND EXISTS (
              SELECT 1 FROM workout_set s
              JOIN workout_session_exercise e ON e.id = s.session_exercise_id
              WHERE e.session_id = workout_session.id AND s.set_state != 'NotStated'
          )
          AND EXISTS (
              SELECT 1 FROM workout_set s
              JOIN workout_session_exercise e ON e.id = s.session_exercise_id
              WHERE e.session_id = workout_session.id AND s.set_state != 'Completed'
          )
        ORDER BY started_at DESC
        LIMIT 1
        """,
    )
    fun observeActiveSession(): Flow<WorkoutSessionEntity?>

    @Query(
        "SELECT * FROM workout_session_exercise WHERE session_id = :sessionId ORDER BY position",
    )
    fun observeExercises(sessionId: Long): Flow<List<WorkoutSessionExerciseEntity>>

    @Query(
        """
        SELECT * FROM workout_set
        WHERE session_exercise_id IN (:sessionExerciseIds)
        ORDER BY position
        """,
    )
    fun observeSetsForExercises(sessionExerciseIds: List<Long>): Flow<List<WorkoutSetEntity>>

    @Query(
        """
        SELECT e.exercise_id AS exerciseId,
               s.started_at AS startedAt,
               s.body_weight AS bodyWeightHundredths
        FROM workout_session_exercise e
        JOIN workout_session s ON s.id = e.session_id
        WHERE s.duration_seconds IS NOT NULL
          AND s.started_at < :before
          AND e.exercise_id IN (:exerciseIds)
        """,
    )
    suspend fun completedExerciseBodyWeights(
        before: LocalDateTime,
        exerciseIds: List<String>,
    ): List<CompletedExerciseBodyWeight>

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
        SET plan_name = :planName, note = :note, started_at = :startedAt,
            duration_seconds = :durationSeconds, body_weight = :bodyWeightHundredths
        WHERE id = :id
        """,
    )
    suspend fun updateSessionHeader(
        id: Long,
        planName: String,
        note: String?,
        startedAt: LocalDateTime,
        durationSeconds: Int?,
        bodyWeightHundredths: Int?,
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
            startedAt = write.header.startedAt,
            durationSeconds = write.header.durationSeconds,
            bodyWeightHundredths = write.header.bodyWeightHundredths,
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
