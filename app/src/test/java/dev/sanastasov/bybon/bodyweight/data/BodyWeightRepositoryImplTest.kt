package dev.sanastasov.bybon.bodyweight.data

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncOpKind
import dev.sanastasov.bybon.workout.data.FakeWeightDao
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BodyWeightRepositoryImplTest {

    @Test
    fun `same-day save replaces the row and enqueues an upsert`() = runTest {
        val repository = repository()
        val date = LocalDate.of(2026, 11, 8)

        repository.insert(BodyWeightEntry(date, BodyWeight(6500)))
        repository.insert(BodyWeightEntry(date, BodyWeight(6550)))

        val stored = repository.entries().first().single()
        assert(stored.weight.value == 6550)
        val op = repository.pendingOps().single()
        assert(op.date == date)
        assert(op.kind == WeightSyncOpKind.Upsert)
        assert(op.weight == 6550)
    }

    @Test
    fun `delete removes the row and enqueues a delete`() = runTest {
        val repository = repository()
        val entry = BodyWeightEntry(LocalDate.of(2026, 11, 8), BodyWeight(6500))
        repository.insert(entry)

        repository.deleteEntry(entry)

        assert(repository.entries().first().isEmpty())
        val op = repository.pendingOps().single()
        assert(op.date == entry.date)
        assert(op.kind == WeightSyncOpKind.Delete)
        assert(op.weight == null)
    }

    @Test
    fun `saving or deleting a weight enqueues one immediate sync`() = runTest {
        var calls = 0
        val repository = BodyWeightRepositoryImpl(
            FakeWeightDao(),
            FakeDietPhaseDao(),
            FakeWeightSyncDao(),
        ) { calls += 1 }
        val entry = BodyWeightEntry(LocalDate.of(2026, 11, 8), BodyWeight(6500))

        repository.insert(entry)
        repository.deleteEntry(entry)
        repository.replaceAll(emptyList())

        assert(calls == 2)
    }

    private fun repository(): BodyWeightRepositoryImpl = BodyWeightRepositoryImpl(
        FakeWeightDao(),
        FakeDietPhaseDao(),
        FakeWeightSyncDao(),
    )
}

private class FakeDietPhaseDao : DietPhaseDao {
    private val phases = MutableStateFlow<List<DietPhaseEntity>>(emptyList())

    override fun phases(): Flow<List<DietPhaseEntity>> = phases

    override suspend fun upsert(entity: DietPhaseEntity) {
        phases.update { current -> current.filterNot { it.id == entity.id } + entity }
    }
}

private class FakeWeightSyncDao : WeightSyncDao {
    private val ops = MutableStateFlow<List<WeightSyncOpEntity>>(emptyList())
    private val states = MutableStateFlow<Map<String, WeightSyncStateEntity>>(emptyMap())

    override fun observeOps(): Flow<List<WeightSyncOpEntity>> = ops

    override suspend fun ops(): List<WeightSyncOpEntity> = ops.value

    override suspend fun upsertOp(op: WeightSyncOpEntity) {
        ops.update { current -> current.filterNot { it.date == op.date } + op }
    }

    override suspend fun clearOps() {
        ops.value = emptyList()
    }

    override suspend fun deleteOps(dates: List<LocalDate>) {
        ops.update { current -> current.filterNot { it.date in dates } }
    }

    override fun observeState(databaseName: String): Flow<WeightSyncStateEntity?> =
        states.map { it[databaseName] }

    override suspend fun state(databaseName: String): WeightSyncStateEntity? =
        states.value[databaseName]

    override suspend fun upsertState(state: WeightSyncStateEntity) {
        states.update { it + (state.databaseName to state) }
    }
}
