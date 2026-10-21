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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WorkoutsRepositoryImpl internal constructor(
    private val exerciseDao: ExerciseDao,
    private val catalog: List<ExerciseDefinition> = catalogExercises,
    private val repdbCommit: String = repdbCatalogCommit,
) : WorkoutsRepository {

    private val plans = MutableStateFlow(listOf(fullBodyA, fullBodyB, upperBodyA))
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
            plans.map { allPlans ->
                when (filter) {
                    WorkoutPlansFilter.UnarchivedPlans -> allPlans.filter { !it.isArchived }
                    WorkoutPlansFilter.AllPlans -> allPlans
                }
            },
        )
    }

    override suspend fun archivePlan(planId: WorkoutPlanId, archived: Boolean) {
        ensureCatalog()
        plans.update { list ->
            list.map { plan ->
                if (plan.id == planId) plan.copy(isArchived = archived) else plan
            }
        }
    }

    override suspend fun updatePlan(
        planId: WorkoutPlanId,
        transform: (WorkoutPlan) -> WorkoutPlan,
    ) {
        ensureCatalog()
        plans.update { list ->
            list.map { plan ->
                if (plan.id == planId) transform(plan) else plan
            }
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
        val addedExercises = seedLock.withLock {
            seedLocked()
            if (exercises.isEmpty()) {
                emptyList()
            } else {
                val added = exercises.withoutExistingIds(exerciseDao.ids().toSet()) { it.id }
                if (added.isNotEmpty()) {
                    exerciseDao.upsertAll(added.map { it.toEntity(source = ExerciseSource.IMPORT) })
                }
                added
            }
        }
        val addedPlans = this.plans.appendWithoutExisting(plans) { it.id }
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
        if (exerciseDao.catalogCommit(REPDB_CATALOG_ROW_ID) != repdbCommit) {
            val protectedIds = exerciseDao.idsWhereSourceIsNot(ExerciseSource.SHIPPED).toSet()
            val rows = catalog
                .map { it.toEntity(source = ExerciseSource.SHIPPED) }
                .filter { it.id !in protectedIds }
            if (rows.isNotEmpty()) {
                exerciseDao.upsertAll(rows)
            }
            exerciseDao.upsertCatalog(RepdbCatalogEntity(commit = repdbCommit))
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
