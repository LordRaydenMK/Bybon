package dev.sanastasov.bybon.workout.data

import dev.sanastasov.bybon.bodyweight.data.WeightDao
import dev.sanastasov.bybon.bodyweight.data.WeightEntryEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeWeightDao(
    initial: List<WeightEntryEntity> = emptyList(),
) : WeightDao {

    private val entries = MutableStateFlow(initial)

    override fun weightEntries(limit: Int): Flow<List<WeightEntryEntity>> =
        entries.map { it.take(limit) }

    override fun findById(date: LocalDate): WeightEntryEntity? =
        entries.value.firstOrNull { it.date == date }

    override suspend fun insertWeight(entry: WeightEntryEntity) {
        entries.value = entries.value.filterNot { it.date == entry.date } + entry
    }

    override suspend fun deleteEntry(entry: WeightEntryEntity) {
        entries.value = entries.value.filterNot { it.date == entry.date }
    }
}
