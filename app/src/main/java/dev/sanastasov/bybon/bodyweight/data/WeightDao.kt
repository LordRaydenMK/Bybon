package dev.sanastasov.bybon.bodyweight.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {

    @Query("SELECT * FROM weight_entry ORDER BY date DESC LIMIT :limit")
    fun weightEntries(limit: Int = Int.MAX_VALUE): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM weight_entry WHERE date = :date")
    fun findById(date: LocalDate): WeightEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(entry: WeightEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeights(entries: List<WeightEntryEntity>)

    @Query("DELETE FROM weight_entry WHERE date = :date")
    suspend fun deleteByDate(date: LocalDate)

    @Query("DELETE FROM weight_entry")
    suspend fun deleteAllWeights()
}
