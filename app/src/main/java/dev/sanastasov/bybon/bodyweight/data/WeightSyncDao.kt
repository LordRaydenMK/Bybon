package dev.sanastasov.bybon.bodyweight.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
@Suppress("TooManyFunctions")
interface WeightSyncDao {

    @Query("SELECT * FROM weight_sync_op")
    fun observeOps(): Flow<List<WeightSyncOpEntity>>

    @Query("SELECT * FROM weight_sync_op")
    suspend fun ops(): List<WeightSyncOpEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOp(op: WeightSyncOpEntity)

    @Query("DELETE FROM weight_sync_op")
    suspend fun clearOps()

    @Query("DELETE FROM weight_sync_op WHERE date IN (:dates)")
    suspend fun deleteOps(dates: List<LocalDate>)

    @Query("SELECT * FROM weight_sync_state WHERE databaseName = :databaseName")
    fun observeState(databaseName: String): Flow<WeightSyncStateEntity?>

    @Query("SELECT * FROM weight_sync_state WHERE databaseName = :databaseName")
    suspend fun state(databaseName: String): WeightSyncStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertState(state: WeightSyncStateEntity)

    @Query("SELECT * FROM weight_sync_clash WHERE databaseName = :databaseName")
    fun observeClashes(databaseName: String): Flow<List<WeightSyncClashEntity>>

    @Query("SELECT * FROM weight_sync_clash WHERE databaseName = :databaseName")
    suspend fun clashes(databaseName: String): List<WeightSyncClashEntity>

    @Query("DELETE FROM weight_sync_clash WHERE databaseName = :databaseName")
    suspend fun deleteClashes(databaseName: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClashes(clashes: List<WeightSyncClashEntity>)

    @Transaction
    suspend fun replaceState(state: WeightSyncStateEntity, clashes: List<WeightSyncClashEntity>) {
        upsertState(state)
        deleteClashes(state.databaseName)
        if (clashes.isNotEmpty()) insertClashes(clashes)
    }
}
