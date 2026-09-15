package dev.sanastasov.bybon.bodyweight.data

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DietPhaseDao {

    @Query("SELECT * FROM diet_phase ORDER BY id DESC")
    fun phases(): Flow<List<DietPhaseEntity>>

    @Upsert
    suspend fun upsert(entity: DietPhaseEntity)
}
