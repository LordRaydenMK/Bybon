package dev.sanastasov.bybon.weight.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {

    @Query("SELECT * FROM weight_entry ORDER BY date DESC")
    fun weightEntries(): Flow<List<WeightEntryEntity>>

    @Insert
    suspend fun insertWeight(entry: WeightEntryEntity)
}
