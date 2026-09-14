package dev.sanastasov.bybon.bodyweight.data

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.bodyweight.domain.BodyWeightRepository
import dev.sanastasov.bybon.bodyweight.domain.DietPhase
import dev.sanastasov.bybon.bodyweight.domain.DietPhaseKind
import dev.sanastasov.bybon.bodyweight.domain.DietPhaseRecord
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BodyWeightRepositoryImpl(
    private val weightEntryDao: WeightDao,
    private val dietPhaseDao: DietPhaseDao,
    private val today: () -> LocalDate = { LocalDate.now() },
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

    override fun openPhase(): Flow<DietPhaseRecord?> = dietPhaseDao.openPhase().map { entity ->
        entity?.toRecord()
    }

    override suspend fun apply(phase: DietPhase) {
        dietPhaseDao.endOpenPhases(today())
        dietPhaseDao.insert(phase.toEntity())
    }

    override suspend fun clear() {
        dietPhaseDao.endOpenPhases(today())
    }
}

private fun DietPhase.toEntity(): DietPhaseEntity = DietPhaseEntity(
    kind = kind.name,
    startDate = startDate,
    startWeight = startWeight.value,
    targetWeight = targetWeight.value,
    durationWeeks = durationWeeks,
    endedAt = null,
)

private fun DietPhaseEntity.toRecord(): DietPhaseRecord = DietPhaseRecord(
    id = id,
    phase = DietPhase(
        kind = DietPhaseKind.valueOf(kind),
        startDate = startDate,
        startWeight = BodyWeight(startWeight),
        targetWeight = BodyWeight(targetWeight),
        durationWeeks = durationWeeks,
    ),
    endedAt = endedAt,
)
