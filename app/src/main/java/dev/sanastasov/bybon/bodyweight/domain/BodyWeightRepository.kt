@file:Suppress("TooManyFunctions")

package dev.sanastasov.bybon.bodyweight.domain

import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface BodyWeightRepository {

    fun entries(): Flow<List<BodyWeightEntry>>

    suspend fun insert(entry: BodyWeightEntry)

    suspend fun deleteEntry(entry: BodyWeightEntry)

    suspend fun replaceAll(entries: List<BodyWeightEntry>)

    suspend fun addAll(entries: List<BodyWeightEntry>)

    fun observePendingOps(): Flow<List<WeightSyncOp>>

    suspend fun pendingOps(): List<WeightSyncOp>

    suspend fun clearPendingOps()

    suspend fun removePendingOps(dates: List<LocalDate>)

    fun observeSyncState(databaseName: String): Flow<WeightSyncState?>

    suspend fun syncState(databaseName: String): WeightSyncState?

    suspend fun saveSyncState(state: WeightSyncState)

    fun phases(): Flow<List<DietPhaseRecord>>

    suspend fun updatePhase(record: DietPhaseRecord)

    fun openPhase(): Flow<DietPhaseRecord?> =
        phases().map { records -> records.firstOrNull { it.endedAt == null } }

    suspend fun endPhase(record: DietPhaseRecord, endedAt: LocalDate) {
        updatePhase(record.copy(endedAt = endedAt))
    }
}
