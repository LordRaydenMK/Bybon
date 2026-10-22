package dev.sanastasov.bybon.workout.data

import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.ImportHistoryResult
import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutPlansFilter
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutSessionId
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import dev.sanastasov.bybon.workout.domain.catalogExercises
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import dev.sanastasov.bybon.workout.domain.repdbCatalogCommit
import dev.sanastasov.bybon.workout.domain.upperBodyA
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val builtInPlans = listOf(fullBodyA, fullBodyB, upperBodyA)

class WorkoutsRepositoryImpl internal constructor(
    private val exerciseDao: ExerciseDao,
    private val workoutPlanDao: WorkoutPlanDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val catalog: List<ExerciseDefinition> = catalogExercises,
    private val repdbCommit: String = repdbCatalogCommit,
) : WorkoutsRepository {

    private val seedLock = Mutex()

    @Volatile
    private var seededCommit: String? = null

    override fun exercises(): Flow<List<ExerciseDefinition>> = flow {
        ensureCatalog()
        emitAll(exerciseDao.observeAll().map { entities -> entities.map { it.toDefinition() } })
    }

    override fun workoutPlans(filter: WorkoutPlansFilter): Flow<List<WorkoutPlan>> = flow {
        ensureCatalog()
        emitAll(
            combine(
                workoutPlanDao.observePlans(),
                workoutPlanDao.observePlanExercises(),
                exerciseDao.observeAll(),
            ) { plans, lines, exercises ->
                val mapped = plans.toDomainPlans(lines, exercises)
                when (filter) {
                    WorkoutPlansFilter.UnarchivedPlans -> mapped.filter { !it.isArchived }
                    WorkoutPlansFilter.AllPlans -> mapped
                }
            },
        )
    }

    override suspend fun archivePlan(planId: WorkoutPlanId, archived: Boolean) {
        seedLock.withLock {
            seedLocked()
            workoutPlanDao.setArchived(planId.id, archived)
        }
    }

    override suspend fun updatePlan(
        planId: WorkoutPlanId,
        transform: (WorkoutPlan) -> WorkoutPlan,
    ) {
        seedLock.withLock {
            seedLocked()
            val entity = requireNotNull(workoutPlanDao.planById(planId.id)) {
                "Plan ${planId.id} does not exist"
            }
            val lines = workoutPlanDao.exercisesForPlan(planId.id)
            val exercises = exerciseDao.observeAll().first()
                .map { it.toDefinition() }
                .associateBy { it.id }
            val current = entity.toDomain(lines, exercises)
            val updated = transform(current)
            workoutPlanDao.replacePlan(updated.toEntity(), updated.toExerciseEntities())
        }
    }

    override suspend fun updateWorkout(session: WorkoutSession) {
        seedLock.withLock {
            seedLocked()
            val existing = workoutSessionDao.openSessionForPlan(session.planId.id)
            if (existing != null) {
                val current = existing.toDomainSession()
                if (session.state is WorkoutState.NotStarted &&
                    current.state is WorkoutState.InProgress
                ) {
                    return
                }
                workoutSessionDao.replaceSessionTree(existing.id, session.toWrite(existing.id))
            } else {
                workoutSessionDao.insertSessionTree(session.toWrite())
            }
        }
    }

    override suspend fun deleteWorkout(sessionId: WorkoutSessionId) {
        seedLock.withLock {
            seedLocked()
            workoutSessionDao.deleteSession(sessionId.planId.id, sessionId.startedAt)
        }
    }

    override fun workoutSessions(): Flow<List<WorkoutSession>> = flow {
        ensureCatalog()
        emitAll(
            combine(
                workoutSessionDao.observeSessions(),
                workoutSessionDao.observeSessionExercises(),
                workoutSessionDao.observeSets(),
                exerciseDao.observeAll(),
            ) { sessions, lines, sets, exercises ->
                sessions.toDomainSessions(lines, sets, exercises)
            },
        )
    }

    override suspend fun importHistory(
        plans: List<WorkoutPlan>,
        sessions: List<WorkoutSession>,
        exercises: List<ExerciseDefinition>,
    ): ImportHistoryResult {
        seedLock.withLock {
            seedLocked()
            val addedExercises = exercises.withoutExistingIds(exerciseDao.ids().toSet()) { it.id }
            val addedPlans = plans.withoutExistingIds(workoutPlanDao.ids().toSet()) { it.id.id }
            val existingSessionIds = workoutSessionDao.sessionIdentities()
                .map { WorkoutSessionId(WorkoutPlanId(it.planId), it.startedAt) }
                .toSet()
            val addedSessions = sessions.withoutExistingIds(existingSessionIds) { it.id }
            workoutSessionDao.importAdded(
                exercises = addedExercises.map { it.toEntity(source = ExerciseSource.IMPORT) },
                plans = addedPlans.map { it.toEntity() },
                planExercises = addedPlans.flatMap { it.toExerciseEntities() },
                sessions = addedSessions.map { it.toWrite() },
            )
            return ImportHistoryResult(
                plans = addedPlans,
                sessions = addedSessions,
                exercises = addedExercises,
                sessionsSkipped = sessions.size - addedSessions.size,
            )
        }
    }

    private suspend fun ensureCatalog() {
        if (seededCommit == repdbCommit) return
        seedLock.withLock { seedLocked() }
    }

    private suspend fun seedLocked() {
        if (seededCommit == repdbCommit) return
        if (exerciseDao.catalogCommit() != repdbCommit) {
            val protectedIds = exerciseDao.idsWhereSourceIsNot(ExerciseSource.SHIPPED).toSet()
            val rows = catalog
                .map { it.toEntity(source = ExerciseSource.SHIPPED) }
                .filter { it.id !in protectedIds }
            if (rows.isNotEmpty()) {
                exerciseDao.upsertAll(rows)
            }
            exerciseDao.insertCatalog(RepdbCatalogEntity(commit = repdbCommit))
        }
        val existingPlans = workoutPlanDao.ids().toSet()
        val newPlans = builtInPlans.filter { it.id.id !in existingPlans }
        if (newPlans.isNotEmpty()) {
            workoutPlanDao.insertPlansWithExercises(
                newPlans.map { it.toEntity() },
                newPlans.flatMap { it.toExerciseEntities() },
            )
        }
        seededCommit = repdbCommit
    }

    private suspend fun WorkoutSessionEntity.toDomainSession(): WorkoutSession {
        val lines = workoutSessionDao.exercisesForSession(id)
        val sets = if (lines.isEmpty()) {
            emptyList()
        } else {
            workoutSessionDao.setsForExercises(lines.map { it.id })
        }
        val exercises = exerciseDao.observeAll().first()
        return checkNotNull(
            listOf(this).toDomainSessions(lines, sets, exercises).singleOrNull(),
        ) { "Session $id has no exercises" }
    }
}
