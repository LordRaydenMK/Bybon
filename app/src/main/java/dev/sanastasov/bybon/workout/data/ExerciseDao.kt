package dev.sanastasov.bybon.workout.data

import androidx.room3.Dao
import androidx.room3.Insert
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

    @Query("SELECT repdb_commit FROM repdb_catalog ORDER BY id DESC LIMIT 1")
    suspend fun catalogCommit(): String?

    @Upsert
    suspend fun upsertAll(entities: List<ExerciseEntity>)

    @Insert
    suspend fun insertCatalog(catalog: RepdbCatalogEntity)
}
