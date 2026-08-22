package dev.sanastasov.bybon.weight.data

import dev.sanastasov.bybon.weight.BodyWeight
import dev.sanastasov.bybon.weight.BodyWeightEntry
import dev.sanastasov.bybon.weight.WeightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WeightRepositoryImpl(
    private val weightEntryDao: WeightDao
) : WeightRepository {

    override fun entries(): Flow<List<BodyWeightEntry>> = weightEntryDao.weightEntries()
        .map { entries -> entries.map { BodyWeightEntry(it.date, BodyWeight(it.weight)) } }

    override suspend fun insert(entry: BodyWeightEntry) {
        val dto = WeightEntryEntity(entry.date, entry.weight.value)
        weightEntryDao.insertWeight(dto)
    }
}
