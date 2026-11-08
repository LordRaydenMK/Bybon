@file:Suppress("TooManyFunctions")

package dev.sanastasov.bybon.bodyweight.data

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.bodyweight.domain.BodyWeightRepository
import dev.sanastasov.bybon.bodyweight.domain.DietPhase
import dev.sanastasov.bybon.bodyweight.domain.DietPhaseKind
import dev.sanastasov.bybon.bodyweight.domain.DietPhaseRecord
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncOp
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncOpKind
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncState
import dev.sanastasov.bybon.bodyweight.domain.durationWeeksOrNull
import dev.sanastasov.bybon.bodyweight.domain.kind
import dev.sanastasov.bybon.bodyweight.domain.requireValid
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BodyWeightRepositoryImpl(
    private val weightEntryDao: WeightDao,
    private val dietPhaseDao: DietPhaseDao,
    private val weightSyncDao: WeightSyncDao,
) : BodyWeightRepository {

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

    override suspend fun replaceAll(entries: List<BodyWeightEntry>) {
        weightEntryDao.deleteAllWeights()
        if (entries.isNotEmpty()) {
            weightEntryDao.insertWeights(entries.map { it.toEntity() })
        }
    }

    override suspend fun addAll(entries: List<BodyWeightEntry>) {
        if (entries.isEmpty()) return
        weightEntryDao.insertWeights(entries.map { it.toEntity() })
    }

    override fun observePendingOps(): Flow<List<WeightSyncOp>> =
        weightSyncDao.observeOps().map { ops -> ops.map { it.toOp() } }

    override suspend fun pendingOps(): List<WeightSyncOp> = weightSyncDao.ops().map { it.toOp() }

    override suspend fun clearPendingOps() {
        weightSyncDao.clearOps()
    }

    override suspend fun removePendingOps(dates: List<LocalDate>) {
        if (dates.isEmpty()) return
        weightSyncDao.deleteOps(dates)
    }

    override fun observeSyncState(databaseName: String): Flow<WeightSyncState?> =
        weightSyncDao.observeState(databaseName).map { it?.toState() }

    override suspend fun syncState(databaseName: String): WeightSyncState? =
        weightSyncDao.state(databaseName)?.toState()

    override suspend fun saveSyncState(state: WeightSyncState) {
        weightSyncDao.upsertState(state.toEntity())
    }

    override fun phases(): Flow<List<DietPhaseRecord>> = dietPhaseDao.phases().map { entities ->
        entities.map { it.toRecord() }
    }

    override suspend fun updatePhase(record: DietPhaseRecord) {
        dietPhaseDao.upsert(record.toEntity())
    }
}

private fun BodyWeightEntry.toEntity(): WeightEntryEntity = WeightEntryEntity(date, weight.value)

private fun WeightSyncOpEntity.toOp(): WeightSyncOp = WeightSyncOp(
    date = date,
    kind = if (kind == WeightSyncOpEntity.DELETE) {
        WeightSyncOpKind.Delete
    } else {
        WeightSyncOpKind.Upsert
    },
    weight = weight,
)

private fun WeightSyncStateEntity.toState(): WeightSyncState = WeightSyncState(
    databaseName = databaseName,
    lastSyncedUid = lastSyncedUid,
    cursorMillis = cursorMillis,
    lastSuccessMillis = lastSuccessMillis,
    conflictPending = conflictPending,
    summaryCount = summaryCount,
    summaryEarliest = summaryEarliest,
    summaryLatest = summaryLatest,
)

private fun WeightSyncState.toEntity(): WeightSyncStateEntity = WeightSyncStateEntity(
    databaseName = databaseName,
    lastSyncedUid = lastSyncedUid,
    cursorMillis = cursorMillis,
    lastSuccessMillis = lastSuccessMillis,
    conflictPending = conflictPending,
    summaryCount = summaryCount,
    summaryEarliest = summaryEarliest,
    summaryLatest = summaryLatest,
)

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
