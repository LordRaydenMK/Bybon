package dev.sanastasov.bybon.workout.data

import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ExerciseCatalog(
    private val exerciseDao: ExerciseDao,
    private val catalog: List<ExerciseDefinition>,
) {
    private val write = Mutex()

    @Volatile
    private var seeded = false

    fun observe(): Flow<List<ExerciseDefinition>> = flow {
        ensureSeeded()
        emitAll(exerciseDao.observeAll().map { entities -> entities.map { it.toDefinition() } })
    }

    suspend fun import(exercises: List<ExerciseDefinition>): List<ExerciseDefinition> =
        write.withLock {
            seedLocked()
            insertImported(exercises)
        }

    suspend fun ensureSeeded() {
        if (seeded) return
        write.withLock { seedLocked() }
    }

    private suspend fun seedLocked() {
        if (seeded) return
        val protectedIds = exerciseDao.idsWhereSourceIsNot(ExerciseSource.SHIPPED).toSet()
        val rows = catalog
            .map { it.toEntity(source = ExerciseSource.SHIPPED) }
            .filter { it.id !in protectedIds }
        if (rows.isNotEmpty()) {
            exerciseDao.upsertAll(rows)
        }
        seeded = true
    }

    private suspend fun insertImported(
        exercises: List<ExerciseDefinition>,
    ): List<ExerciseDefinition> {
        if (exercises.isEmpty()) return emptyList()
        val added = exercises.withoutExistingIds(exerciseDao.ids().toSet()) { it.id }
        if (added.isNotEmpty()) {
            exerciseDao.insertAll(added.map { it.toEntity(source = ExerciseSource.IMPORT) })
        }
        return added
    }
}
