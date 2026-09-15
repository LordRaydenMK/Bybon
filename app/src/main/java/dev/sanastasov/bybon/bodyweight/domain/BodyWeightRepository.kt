package dev.sanastasov.bybon.bodyweight.domain

import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface BodyWeightRepository {

    fun entries(): Flow<List<BodyWeightEntry>>

    suspend fun insert(entry: BodyWeightEntry)

    suspend fun deleteEntry(entry: BodyWeightEntry)

    fun phases(): Flow<List<DietPhaseRecord>>

    suspend fun updatePhase(record: DietPhaseRecord)

    fun openPhase(): Flow<DietPhaseRecord?> =
        phases().map { records -> records.firstOrNull { it.endedAt == null } }

    suspend fun endPhase(record: DietPhaseRecord, endedAt: LocalDate) {
        updatePhase(record.copy(endedAt = endedAt))
    }
}
