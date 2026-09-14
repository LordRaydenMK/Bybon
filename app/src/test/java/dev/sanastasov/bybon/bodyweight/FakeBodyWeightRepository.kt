package dev.sanastasov.bybon.bodyweight

import dev.sanastasov.bybon.bodyweight.domain.BodyWeightRepository
import dev.sanastasov.bybon.bodyweight.domain.DietPhase
import dev.sanastasov.bybon.bodyweight.domain.DietPhaseRecord
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

    override fun openPhase(): Flow<DietPhaseRecord?> = records.map { all ->
        all.lastOrNull { it.endedAt == null }
    }

    override suspend fun apply(phase: DietPhase) {
        clear()
        records.value = records.value + DietPhaseRecord(nextId++, phase)
    }

    override suspend fun clear() {
        records.value = records.value.map { record ->
            if (record.endedAt == null) record.copy(endedAt = LocalDate.now()) else record
        }
    }
}
