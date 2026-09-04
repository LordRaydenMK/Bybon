package dev.sanastasov.bybon.bodyweight.data

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.bodyweight.domain.BodyWeightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BodyWeightRepositoryImpl(
    private val weightEntryDao: WeightDao
) : BodyWeightRepository {

    override fun entries(): Flow<List<BodyWeightEntry>> = weightEntryDao.weightEntries()
        .map { entries -> entries.map { BodyWeightEntry(it.date, BodyWeight(it.weight)) } }

    override suspend fun insert(entry: BodyWeightEntry) {
        val dto = WeightEntryEntity(entry.date, entry.weight.value)
        weightEntryDao.insertWeight(dto)
    }

    override suspend fun deleteEntry(entry: BodyWeightEntry) {
        val dto = WeightEntryEntity(entry.date, entry.weight.value)
        weightEntryDao.deleteEntry(dto)
    }
}
