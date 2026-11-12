package dev.sanastasov.bybon.bodyweight.data

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.bodyweight.domain.BodyWeightRepository
import dev.sanastasov.bybon.bodyweight.domain.DietPhase
import dev.sanastasov.bybon.bodyweight.domain.DietPhaseKind
import dev.sanastasov.bybon.bodyweight.domain.DietPhaseRecord
import dev.sanastasov.bybon.bodyweight.domain.WeightBackupStatus
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncCommand
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncResult
import dev.sanastasov.bybon.bodyweight.domain.durationWeeksOrNull
import dev.sanastasov.bybon.bodyweight.domain.kind
import dev.sanastasov.bybon.bodyweight.domain.requireValid
import dev.sanastasov.bybon.bodyweight.sync.WeightCloud
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BodyWeightRepositoryImpl(
    private val weightEntryDao: WeightDao,
    private val dietPhaseDao: DietPhaseDao,
    private val weightSyncDao: WeightSyncDao,
    cloud: WeightCloud,
    databaseName: String,
    clock: Clock = Clock.systemUTC(),
) : BodyWeightRepository {

    private val syncEngine = WeightSyncEngine(
        weights = weightEntryDao,
        syncDao = weightSyncDao,
        cloud = cloud,
        databaseName = databaseName,
        clock = clock,
    )

    override fun entries(): Flow<List<BodyWeightEntry>> = weightEntryDao.weightEntries()
        .map { entries -> entries.map { BodyWeightEntry(it.date, BodyWeight(it.weight)) } }

    override suspend fun insert(entry: BodyWeightEntry) {
        weightEntryDao.insertWeight(WeightEntryEntity(entry.date, entry.weight.value))
        weightSyncDao.upsertOp(
            WeightSyncOpEntity(
                date = entry.date,
                kind = WeightSyncOpEntity.UPSERT,
                weight = entry.weight.value,
            ),
        )
    }

    override suspend fun deleteEntry(entry: BodyWeightEntry) {
        weightEntryDao.deleteByDate(entry.date)
        weightSyncDao.upsertOp(
            WeightSyncOpEntity(
                date = entry.date,
                kind = WeightSyncOpEntity.DELETE,
                weight = null,
            ),
        )
    }

    override fun backupStatus(): Flow<WeightBackupStatus> = syncEngine.status()

    override suspend fun sync(command: WeightSyncCommand): WeightSyncResult =
        syncEngine.sync(command)

    override suspend fun dismissSummary() {
        syncEngine.dismissSummary()
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
