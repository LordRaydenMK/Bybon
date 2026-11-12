package dev.sanastasov.bybon.bodyweight.data

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.bodyweight.sync.RemoteWeight
import dev.sanastasov.bybon.bodyweight.sync.WeightCloud
import dev.sanastasov.bybon.bodyweight.sync.WeightUpload
import dev.sanastasov.bybon.workout.data.FakeWeightDao
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BodyWeightRepositoryImplTest {

    @Test
    fun `same-day save replaces the row and enqueues an upsert`() = runTest {
        val syncDao = FakeWeightSyncDao()
        val repository = repository(syncDao)
        val date = LocalDate.of(2026, 11, 8)

        repository.insert(BodyWeightEntry(date, BodyWeight(6500)))
        repository.insert(BodyWeightEntry(date, BodyWeight(6550)))

        val stored = repository.entries().first().single()
        assert(stored.weight.value == 6550)
        val op = syncDao.observeOps().first().single()
        assert(op.date == date)
        assert(op.kind == WeightSyncOpEntity.UPSERT)
        assert(op.weight == 6550)
        assert(repository.backupStatus().first().pending)
    }

    @Test
    fun `delete removes the row and enqueues a delete`() = runTest {
        val syncDao = FakeWeightSyncDao()
        val repository = repository(syncDao)
        val entry = BodyWeightEntry(LocalDate.of(2026, 11, 8), BodyWeight(6500))
        repository.insert(entry)

        repository.deleteEntry(entry)

        assert(repository.entries().first().isEmpty())
        val op = syncDao.observeOps().first().single()
        assert(op.date == entry.date)
        assert(op.kind == WeightSyncOpEntity.DELETE)
        assert(op.weight == null)
    }

    private fun repository(syncDao: WeightSyncDao = FakeWeightSyncDao()) = BodyWeightRepositoryImpl(
        FakeWeightDao(),
        FakeDietPhaseDao(),
        syncDao,
        UnusedWeightCloud,
    )
}

private class FakeDietPhaseDao : DietPhaseDao {
    private val phases = MutableStateFlow<List<DietPhaseEntity>>(emptyList())

    override fun phases(): Flow<List<DietPhaseEntity>> = phases

    override suspend fun upsert(entity: DietPhaseEntity) {
        phases.update { current -> current.filterNot { it.id == entity.id } + entity }
    }
}

private object UnusedWeightCloud : WeightCloud {
    override suspend fun readAll(uid: String): List<RemoteWeight> = error("No cloud")

    override suspend fun readUpdatedAfter(uid: String, cursorMillis: Long): List<RemoteWeight> =
        error("No cloud")

    override suspend fun write(uid: String, documents: List<WeightUpload>) = error("No cloud")
}
