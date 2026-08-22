package dev.sanastasov.bybon.weight

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

data class WeightEntry(
    val date: LocalDate,
    val weight: Float,
)

interface WeightRepository {

    fun entries(): Flow<List<WeightEntry>>

    suspend fun insert(entry: WeightEntry)
}

object InMemoryWeightRepository : WeightRepository {

    private val dailyEntries = listOf(
        WeightEntry(LocalDate.of(2026, 8, 21), 65.2f),
        WeightEntry(LocalDate.of(2026, 8, 20), 65.2f),
    )

    private val state: MutableStateFlow<List<WeightEntry>> = MutableStateFlow(dailyEntries)


    override fun entries(): Flow<List<WeightEntry>> = state

    override suspend fun insert(entry: WeightEntry) {
        state.update { listOf(entry) + it }
    }
}