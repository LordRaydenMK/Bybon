package dev.sanastasov.bybon.bodyweight

import dev.sanastasov.bybon.bodyweight.domain.BodyWeightRepository
import dev.sanastasov.bybon.bodyweight.domain.DietPhaseRecord
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncOp
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncOpKind
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncState
import java.time.LocalDate
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
    private val ops = MutableStateFlow<List<WeightSyncOp>>(emptyList())
    private val states = MutableStateFlow<Map<String, WeightSyncState>>(emptyMap())
    private var nextId = (initialPhase?.id ?: 0L) + 1

    override fun entries(): Flow<List<BodyWeightEntry>> = entries

    override suspend fun insert(entry: BodyWeightEntry) {
        entries.update { current ->
            (current.filterNot { it.date == entry.date } + entry).sortedByDescending { it.date }
        }
        enqueue(WeightSyncOp(entry.date, WeightSyncOpKind.Upsert, entry.weight.value))
    }

    override suspend fun deleteEntry(entry: BodyWeightEntry) {
        entries.update { current -> current.filterNot { it.date == entry.date } }
        enqueue(WeightSyncOp(entry.date, WeightSyncOpKind.Delete, null))
    }

    override suspend fun replaceAll(entries: List<BodyWeightEntry>) {
        this.entries.value = entries.sortedByDescending { it.date }
    }

    override suspend fun addAll(entries: List<BodyWeightEntry>) {
        if (entries.isEmpty()) return
        this.entries.update { current ->
            val dates = entries.map { it.date }.toSet()
            (current.filterNot { it.date in dates } + entries).sortedByDescending { it.date }
        }
    }

    override fun observePendingOps(): Flow<List<WeightSyncOp>> = ops

    override suspend fun pendingOps(): List<WeightSyncOp> = ops.value

    override suspend fun clearPendingOps() {
        ops.value = emptyList()
    }

    override suspend fun removePendingOps(dates: List<LocalDate>) {
        if (dates.isEmpty()) return
        ops.update { current -> current.filterNot { it.date in dates } }
    }

    override fun observeSyncState(databaseName: String): Flow<WeightSyncState?> =
        states.map { it[databaseName] }

    override suspend fun syncState(databaseName: String): WeightSyncState? =
        states.value[databaseName]

    override suspend fun saveSyncState(state: WeightSyncState) {
        states.update { it + (state.databaseName to state) }
    }

    private fun enqueue(op: WeightSyncOp) {
        ops.update { current -> current.filterNot { it.date == op.date } + op }
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
