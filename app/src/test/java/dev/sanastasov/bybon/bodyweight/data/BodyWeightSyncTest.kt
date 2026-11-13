package dev.sanastasov.bybon.bodyweight.data

import dev.sanastasov.bybon.account.domain.UserId
import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.bodyweight.domain.WeightConflict
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncCommand
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncResult
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncStart
import dev.sanastasov.bybon.bodyweight.sync.RemoteWeight
import dev.sanastasov.bybon.bodyweight.sync.WeightCloud
import dev.sanastasov.bybon.bodyweight.sync.WeightUpload
import dev.sanastasov.bybon.workout.data.FakeWeightDao
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BodyWeightSyncTest {

    private val jan1 = LocalDate.of(2026, 1, 1)

    @Test
    fun `a different account aborts and leaves the phone`() = runTest {
        val syncDao = FakeWeightSyncDao()
        val cloud = FakeWeightCloud()
        syncDao.upsertState(state(lastSyncedUid = "old"))
        val repository = repository(syncDao, cloud)
        repository.insert(entry(jan1, 6500))

        val result = repository.sync(WeightSyncCommand(UserId("uid"), WeightSyncStart.SignIn))

        val conflict = (result as WeightSyncResult.Conflict).conflict as WeightConflict.DifferentAccount
        assert(conflict.previous == UserId("old"))
        assert(cloud.writes.isEmpty())
        assert(repository.entries().first().single().weight.value == 6500)
        assert(syncDao.observeState().first()?.lastSyncedUid == "old")
        assert(repository.backupStatus().first().pending)
    }

    @Test
    fun `an empty cloud on sign-in uploads the phone`() = runTest {
        val syncDao = FakeWeightSyncDao()
        val cloud = FakeWeightCloud()
        val repository = repository(syncDao, cloud)
        repository.insert(entry(jan1, 6500))

        val result = repository.sync(WeightSyncCommand(UserId("uid"), WeightSyncStart.SignIn))

        assert(result == WeightSyncResult.Completed)
        assert(cloud.writes.single().single().date == jan1)
        assert(!cloud.writes.single().single().deleted)
        assert(syncDao.observeOps().first().isEmpty())
        assert(syncDao.observeState().first()?.lastSyncedUid == "uid")
        assert(repository.backupStatus().first().lastSuccess == Instant.ofEpochMilli(1_000))
    }

    @Test
    fun `a changed weight aborts and leaves the phone`() = runTest {
        val cloud = FakeWeightCloud()
        val repository = repository(FakeWeightSyncDao(), cloud)
        repository.insert(entry(jan1, 6500))
        cloud.all = listOf(
            RemoteWeight(
                date = jan1,
                weight = BodyWeight(6600),
                deleted = false,
                updatedAtMillis = 5,
            ),
        )

        val result = repository.sync(WeightSyncCommand(UserId("uid"), WeightSyncStart.SignIn))

        assert((result as WeightSyncResult.Conflict).conflict == WeightConflict.OverlappingDates)
        assert(cloud.writes.isEmpty())
        assert(repository.entries().first().single().weight.value == 6500)
        assert(repository.backupStatus().first().summary == null)
    }

    @Test
    fun `an incremental pull reads only newer documents`() = runTest {
        val syncDao = FakeWeightSyncDao()
        val cloud = FakeWeightCloud()
        syncDao.upsertState(state(cursorMillis = 10, lastSyncedUid = "uid"))
        val repository = repository(syncDao, cloud)

        repository.sync(WeightSyncCommand(UserId("uid"), WeightSyncStart.Incremental))

        assert(cloud.reads == 0)
        assert(cloud.updatedReads == 1)
    }

    @Test
    fun `a remote miss leaves the pending op`() = runTest {
        val cloud = FakeWeightCloud()
        cloud.fail = true
        val repository = repository(FakeWeightSyncDao(), cloud)
        repository.insert(entry(jan1, 6500))

        val result = repository.sync(WeightSyncCommand(UserId("uid"), WeightSyncStart.SignIn))

        assert(result == WeightSyncResult.Unavailable)
        assert(repository.backupStatus().first().pending)
        assert(repository.entries().first().single().weight.value == 6500)
    }

    @Test
    fun `dismiss summary clears the stored summary`() = runTest {
        val syncDao = FakeWeightSyncDao()
        syncDao.upsertState(
            state().copy(
                summaryCount = 1,
                summaryEarliest = jan1,
                summaryLatest = jan1,
            ),
        )
        val repository = repository(syncDao, FakeWeightCloud())

        assert(repository.backupStatus().first().summary?.count == 1)
        repository.dismissSummary()

        assert(repository.backupStatus().first().summary == null)
    }

    private fun repository(syncDao: WeightSyncDao, cloud: WeightCloud) = BodyWeightRepositoryImpl(
        FakeWeightDao(),
        EmptyDietPhaseDao(),
        syncDao,
        cloud,
        Clock.fixed(Instant.ofEpochMilli(1_000), ZoneOffset.UTC),
    )

    private fun entry(date: LocalDate, weight: Int) = BodyWeightEntry(date, BodyWeight(weight))

    private fun state(cursorMillis: Long? = null, lastSyncedUid: String? = null) =
        WeightSyncStateEntity(
            lastSyncedUid = lastSyncedUid,
            cursorMillis = cursorMillis,
            lastSuccessMillis = null,
            summaryCount = null,
            summaryEarliest = null,
            summaryLatest = null,
        )
}

private class EmptyDietPhaseDao : DietPhaseDao {
    override fun phases(): Flow<List<DietPhaseEntity>> = flowOf(emptyList())

    override suspend fun upsert(entity: DietPhaseEntity) = Unit
}

private class FakeWeightCloud : WeightCloud {
    var all: List<RemoteWeight> = emptyList()
    var updated: List<RemoteWeight> = emptyList()
    var fail: Boolean = false
    val writes = mutableListOf<List<WeightUpload>>()
    var reads = 0
    var updatedReads = 0

    override suspend fun readAll(uid: String): List<RemoteWeight> {
        if (fail) throw IOException("offline")
        reads += 1
        return all
    }

    override suspend fun readUpdatedAfter(uid: String, cursorMillis: Long): List<RemoteWeight> {
        if (fail) throw IOException("offline")
        updatedReads += 1
        return updated
    }

    override suspend fun write(uid: String, documents: List<WeightUpload>) {
        if (fail) throw IOException("offline")
        writes += documents
    }
}
