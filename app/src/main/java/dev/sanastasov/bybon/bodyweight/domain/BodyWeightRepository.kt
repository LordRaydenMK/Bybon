package dev.sanastasov.bybon.bodyweight.domain

import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface BodyWeightRepository {

    fun entries(): Flow<List<BodyWeightEntry>>

    suspend fun insert(entry: BodyWeightEntry)

    suspend fun deleteEntry(entry: BodyWeightEntry)

    fun backupStatus(): Flow<WeightBackupStatus>

    /**
     * Pull and upload, or apply the choice for a stored conflict.
     * A pull that finds a stored conflict returns that choice request and does not read the cloud.
     * Resolve throws when nothing is stored.
     * A known remote failure returns [WeightSyncResult.Unavailable]. Anything else throws.
     */
    suspend fun sync(command: WeightSyncCommand): WeightSyncResult

    suspend fun dismissSummary()

    fun phases(): Flow<List<DietPhaseRecord>>

    suspend fun updatePhase(record: DietPhaseRecord)

    fun openPhase(): Flow<DietPhaseRecord?> =
        phases().map { records -> records.firstOrNull { it.endedAt == null } }

    suspend fun endPhase(record: DietPhaseRecord, endedAt: LocalDate) {
        updatePhase(record.copy(endedAt = endedAt))
    }
}
