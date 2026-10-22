package dev.sanastasov.bybon.workout.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeExerciseDao : ExerciseDao {

    private val rows = MutableStateFlow<List<ExerciseEntity>>(emptyList())
    private var catalogCommit: String? = null

    override fun observeAll(): Flow<List<ExerciseEntity>> = rows

    override suspend fun ids(): List<String> = rows.value.map { it.id }

    override suspend fun idsWhereSourceIsNot(source: String): List<String> =
        rows.value.filter { it.source != source }.map { it.id }

    override suspend fun catalogCommit(): String? = catalogCommit

    override suspend fun upsertAll(entities: List<ExerciseEntity>) {
        if (entities.isEmpty()) return
        val current = rows.value.toMutableList()
        for (entity in entities) {
            val index = current.indexOfFirst { it.id == entity.id }
            if (index >= 0) {
                current[index] = entity
            } else {
                current += entity
            }
        }
        rows.value = current
    }

    override suspend fun insertCatalog(catalog: RepdbCatalogEntity) {
        catalogCommit = catalog.commit
    }
}
