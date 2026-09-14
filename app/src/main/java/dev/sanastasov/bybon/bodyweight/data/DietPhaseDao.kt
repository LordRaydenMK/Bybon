package dev.sanastasov.bybon.bodyweight.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface DietPhaseDao {

    @Query("SELECT * FROM diet_phase WHERE endedAt IS NULL LIMIT 1")
    fun openPhase(): Flow<DietPhaseEntity?>

    @Insert
    suspend fun insert(entity: DietPhaseEntity)

    @Query("UPDATE diet_phase SET endedAt = :endedAt WHERE endedAt IS NULL")
    suspend fun endOpenPhases(endedAt: LocalDate)
}
