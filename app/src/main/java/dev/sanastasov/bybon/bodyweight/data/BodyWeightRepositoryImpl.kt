package dev.sanastasov.bybon.bodyweight.data

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.bodyweight.domain.BodyWeightRepository
import dev.sanastasov.bybon.bodyweight.domain.DietPhase
import dev.sanastasov.bybon.bodyweight.domain.DietPhaseKind
import dev.sanastasov.bybon.bodyweight.domain.DietPhaseRecord
import dev.sanastasov.bybon.bodyweight.domain.durationWeeksOrNull
import dev.sanastasov.bybon.bodyweight.domain.kind
import dev.sanastasov.bybon.bodyweight.domain.requireValid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BodyWeightRepositoryImpl(
    private val weightEntryDao: WeightDao,
    private val dietPhaseDao: DietPhaseDao,
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

    override fun phases(): Flow<List<DietPhaseRecord>> = dietPhaseDao.phases().map { entities ->
        entities.map { it.toRecord() }
    }

    override suspend fun updatePhase(record: DietPhaseRecord) {
        dietPhaseDao.upsert(record.toEntity())
    }
}

private fun DietPhaseRecord.toEntity(): DietPhaseEntity = DietPhaseEntity(
    id = id,
    kind = phase.kind.name,
    startDate = phase.startDate,
    startWeight = phase.startWeight.value,
    targetWeight = phase.targetWeight.value,
    durationWeeks = phase.durationWeeksOrNull,
    endedAt = endedAt,
)

private fun DietPhaseEntity.toRecord(): DietPhaseRecord = DietPhaseRecord(
    id = id,
    phase = DietPhase.create(
        DietPhaseKind.valueOf(kind),
        startDate,
        BodyWeight(startWeight),
        BodyWeight(targetWeight),
        durationWeeks,
    ).requireValid(),
    endedAt = endedAt,
)
