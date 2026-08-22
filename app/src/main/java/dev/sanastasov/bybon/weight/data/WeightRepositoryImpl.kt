package dev.sanastasov.bybon.weight.data

import dev.sanastasov.bybon.weight.WeightEntry
import dev.sanastasov.bybon.weight.WeightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WeightRepositoryImpl(
    private val weightEntryDao: WeightDao
) : WeightRepository {

    override fun entries(): Flow<List<WeightEntry>> = weightEntryDao.weightEntries()
        .map { entries -> entries.map { WeightEntry(it.date, it.weight) } }

    override suspend fun insert(entry: WeightEntry) {
        val dto = WeightEntryEntity(entry.date, entry.weight)
        weightEntryDao.insertWeight(dto)
    }
}
