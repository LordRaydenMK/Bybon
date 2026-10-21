package dev.sanastasov.bybon.workout.data

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    // rowid keeps shipped seed order and appends later imports.
    @Query("SELECT * FROM exercise ORDER BY rowid")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT id FROM exercise")
    suspend fun ids(): List<String>

    @Query("SELECT id FROM exercise WHERE source != :source")
    suspend fun idsWhereSourceIsNot(source: String): List<String>

    @Query("SELECT repdb_commit FROM repdb_catalog WHERE id = :id")
    suspend fun catalogCommit(id: Int): String?

    @Upsert
    suspend fun upsertAll(entities: List<ExerciseEntity>)

    @Upsert
    suspend fun upsertCatalog(catalog: RepdbCatalogEntity)
}
