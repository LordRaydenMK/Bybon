package dev.sanastasov.bybon.bodyweight

import dev.sanastasov.bybon.bodyweight.domain.BodyWeightRepository
import dev.sanastasov.bybon.bodyweight.domain.DietPhaseRecord
import dev.sanastasov.bybon.bodyweight.domain.WeightBackupStatus
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncCommand
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeBodyWeightRepository(
    initialEntries: List<BodyWeightEntry> = emptyList(),
    initialPhase: DietPhaseRecord? = null,
) : BodyWeightRepository {

    private val entries = MutableStateFlow(initialEntries.sortedByDescending { it.date })
    private val records = MutableStateFlow(listOfNotNull(initialPhase))
    val backup = MutableStateFlow(WeightBackupStatus())
    val syncs = mutableListOf<WeightSyncCommand>()
    var nextSyncResult: WeightSyncResult = WeightSyncResult.Completed
    private var nextId = (initialPhase?.id ?: 0L) + 1

    override fun entries(): Flow<List<BodyWeightEntry>> = entries

    override suspend fun insert(entry: BodyWeightEntry) {
        entries.update { current ->
            (current.filterNot { it.date == entry.date } + entry).sortedByDescending { it.date }
        }
    }

    override suspend fun deleteEntry(entry: BodyWeightEntry) {
        entries.update { current -> current.filterNot { it.date == entry.date } }
    }

    override fun backupStatus(): Flow<WeightBackupStatus> = backup

    override suspend fun sync(command: WeightSyncCommand): WeightSyncResult {
        syncs += command
        return nextSyncResult
    }

    override suspend fun dismissSummary() {
        backup.update { it.copy(summary = null) }
    }

    override fun phases(): Flow<List<DietPhaseRecord>> = records.map { all ->
        all.sortedByDescending { it.id }
    }

    override suspend fun updatePhase(record: DietPhaseRecord) {
        if (record.id == 0L) {
            records.update { it + record.copy(id = nextId++) }
        } else {
            records.update { current ->
                current.map { existing -> if (existing.id == record.id) record else existing }
            }
        }
    }
}
