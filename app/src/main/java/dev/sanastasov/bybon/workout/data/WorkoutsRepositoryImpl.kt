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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val builtInPlans = listOf(fullBodyA, fullBodyB, upperBodyA)

class WorkoutsRepositoryImpl internal constructor(
    private val exerciseDao: ExerciseDao,
    private val workoutPlanDao: WorkoutPlanDao,
    private val catalog: List<ExerciseDefinition> = catalogExercises,
    private val repdbCommit: String = repdbCatalogCommit,
) : WorkoutsRepository {

    private val sessions = MutableStateFlow<List<WorkoutSession>>(emptyList())
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
            val entity = workoutPlanDao.planById(planId.id) ?: return
            val lines = workoutPlanDao.exercisesForPlan(planId.id)
            val exercises = exerciseDao.observeAll().first()
                .map { it.toDefinition() }
                .associateBy { it.id }
            val current = entity.toDomain(lines, exercises) ?: return
            val updated = transform(current)
            workoutPlanDao.replacePlan(updated.toEntity(), updated.toExerciseEntities())
        }
    }

    override suspend fun updateWorkout(session: WorkoutSession) {
        ensureCatalog()
        sessions.update { list ->
            val index = list.indexOfFirst {
                it.planId == session.planId && it.state !is WorkoutState.Completed
            }
            if (index >= 0) {
                val existing = list[index]
                if (session.state is WorkoutState.NotStarted &&
                    existing.state is WorkoutState.InProgress
                ) {
                    list
                } else {
                    list.toMutableList().apply { set(index, session) }
                }
            } else {
                list + session
            }
        }
    }

    override suspend fun deleteWorkout(sessionId: WorkoutSessionId) {
        ensureCatalog()
        sessions.update { list -> list.filterNot { it.id == sessionId } }
    }

    override fun workoutSessions(): Flow<List<WorkoutSession>> = flow {
        ensureCatalog()
        emitAll(sessions)
    }

    override suspend fun importHistory(
        plans: List<WorkoutPlan>,
        sessions: List<WorkoutSession>,
        exercises: List<ExerciseDefinition>,
    ): ImportHistoryResult {
        val addedExercises: List<ExerciseDefinition>
        val addedPlans: List<WorkoutPlan>
        seedLock.withLock {
            seedLocked()
            addedExercises = if (exercises.isEmpty()) {
                emptyList()
            } else {
                val added = exercises.withoutExistingIds(exerciseDao.ids().toSet()) { it.id }
                if (added.isNotEmpty()) {
                    exerciseDao.upsertAll(added.map { it.toEntity(source = ExerciseSource.IMPORT) })
                }
                added
            }
            addedPlans = plans.withoutExistingIds(workoutPlanDao.ids().toSet()) { it.id.id }
            if (addedPlans.isNotEmpty()) {
                workoutPlanDao.insertPlansWithExercises(
                    addedPlans.map { it.toEntity() },
                    addedPlans.flatMap { it.toExerciseEntities() },
                )
            }
        }
        val addedSessions = this.sessions.appendWithoutExisting(sessions) { it.id }
        return ImportHistoryResult(
            plans = addedPlans,
            sessions = addedSessions,
            exercises = addedExercises,
            sessionsSkipped = sessions.size - addedSessions.size,
        )
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

    private fun <T, I> MutableStateFlow<List<T>>.appendWithoutExisting(
        incoming: List<T>,
        id: (T) -> I,
    ): List<T> {
        var added = emptyList<T>()
        update { current ->
            added = incoming.withoutExisting(current, id)
            current + added
        }
        return added
    }
}
