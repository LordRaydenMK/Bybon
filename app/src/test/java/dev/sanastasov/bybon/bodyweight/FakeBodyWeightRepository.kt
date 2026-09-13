package dev.sanastasov.bybon.bodyweight

import dev.sanastasov.bybon.bodyweight.domain.BodyWeightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeBodyWeightRepository(
    initialEntries: List<BodyWeightEntry> = emptyList(),
) : BodyWeightRepository {

    private val entries = MutableStateFlow(initialEntries.sortedByDescending { it.date })

    override fun entries(): Flow<List<BodyWeightEntry>> = entries

    override suspend fun insert(entry: BodyWeightEntry) {
        entries.update { current ->
            (current.filterNot { it.date == entry.date } + entry).sortedByDescending { it.date }
        }
    }

    override suspend fun deleteEntry(entry: BodyWeightEntry) {
        entries.update { current -> current.filterNot { it.date == entry.date } }
    }
}
