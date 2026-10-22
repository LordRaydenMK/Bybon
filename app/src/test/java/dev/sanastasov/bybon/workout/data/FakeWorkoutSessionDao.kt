package dev.sanastasov.bybon.workout.data

import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeWorkoutSessionDao(
    private val exerciseDao: ExerciseDao = FakeExerciseDao(),
    private val workoutPlanDao: WorkoutPlanDao = FakeWorkoutPlanDao(),
) : WorkoutSessionDao {

    private val sessions = MutableStateFlow<List<WorkoutSessionEntity>>(emptyList())
    private val exercises = MutableStateFlow<List<WorkoutSessionExerciseEntity>>(emptyList())
    private val sets = MutableStateFlow<List<WorkoutSetEntity>>(emptyList())
    private var nextSessionId = 1L
    private var nextExerciseId = 1L
    private var nextSetId = 1L

    override fun observeSessions(): Flow<List<WorkoutSessionEntity>> = sessions

    override fun observeSessionExercises(): Flow<List<WorkoutSessionExerciseEntity>> = exercises

    override fun observeSets(): Flow<List<WorkoutSetEntity>> = sets

    override suspend fun sessionIdentities(): List<WorkoutSessionIdentity> =
        sessions.value.map { WorkoutSessionIdentity(it.planId, it.startedAt) }

    override suspend fun openSessionForPlan(planId: String): WorkoutSessionEntity? =
        sessions.value.firstOrNull { it.planId == planId && it.durationSeconds == null }

    override suspend fun exercisesForSession(sessionId: Long): List<WorkoutSessionExerciseEntity> =
        exercises.value.filter { it.sessionId == sessionId }.sortedBy { it.position }

    override suspend fun setsForExercises(sessionExerciseIds: List<Long>): List<WorkoutSetEntity> =
        sets.value.filter { it.sessionExerciseId in sessionExerciseIds }.sortedBy { it.position }

    override suspend fun insertSession(session: WorkoutSessionEntity): Long {
        val id = if (session.id != 0L) session.id else nextSessionId++
        sessions.value = sessions.value + session.copy(id = id)
        return id
    }

    override suspend fun insertSessionExercise(exercise: WorkoutSessionExerciseEntity): Long {
        val id = if (exercise.id != 0L) exercise.id else nextExerciseId++
        exercises.value = exercises.value + exercise.copy(id = id)
        return id
    }

    override suspend fun insertSets(sets: List<WorkoutSetEntity>) {
        if (sets.isEmpty()) return
        this.sets.value = this.sets.value + sets.map { entity ->
            if (entity.id != 0L) entity else entity.copy(id = nextSetId++)
        }
    }

    override suspend fun updateSessionHeader(
        id: Long,
        planName: String,
        note: String?,
        durationSeconds: Int?,
    ) {
        val current = sessions.value
        val index = current.indexOfFirst { it.id == id }
        if (index < 0) return
        sessions.value = current.toMutableList().apply {
            set(
                index,
                current[index].copy(
                    planName = planName,
                    note = note,
                    durationSeconds = durationSeconds,
                ),
            )
        }
    }

    override suspend fun deleteExercisesForSession(sessionId: Long) {
        val removed = exercises.value.filter { it.sessionId == sessionId }.map { it.id }.toSet()
        exercises.value = exercises.value.filterNot { it.sessionId == sessionId }
        sets.value = sets.value.filterNot { it.sessionExerciseId in removed }
    }

    override suspend fun deleteSession(planId: String, startedAt: LocalDateTime) {
        val removed = sessions.value.filter { session ->
            session.planId == planId && session.startedAt == startedAt
        }
        sessions.value = sessions.value.filterNot { session ->
            session.planId == planId && session.startedAt == startedAt
        }
        removed.forEach { deleteExercisesForSession(it.id) }
    }

    override suspend fun insertImportedExercises(exercises: List<ExerciseEntity>) {
        if (exercises.isEmpty()) return
        exerciseDao.upsertAll(exercises)
    }

    override suspend fun insertImportedPlans(plans: List<WorkoutPlanEntity>) {
        workoutPlanDao.insertPlans(plans)
    }

    override suspend fun insertImportedPlanExercises(exercises: List<WorkoutPlanExerciseEntity>) {
        workoutPlanDao.insertPlanExercises(exercises)
    }
}
