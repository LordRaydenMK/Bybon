package dev.sanastasov.bybon.bodyweight.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightSyncDao {

    @Query("SELECT * FROM weight_sync_op")
    fun observeOps(): Flow<List<WeightSyncOpEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOp(op: WeightSyncOpEntity)

    @Query("DELETE FROM weight_sync_op")
    suspend fun clearOps()

    @Query("DELETE FROM weight_sync_op WHERE date IN (:dates)")
    suspend fun deleteOps(dates: List<LocalDate>)

    @Query("SELECT * FROM weight_sync_state WHERE id = 0")
    fun observeState(): Flow<WeightSyncStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertState(state: WeightSyncStateEntity)
}
