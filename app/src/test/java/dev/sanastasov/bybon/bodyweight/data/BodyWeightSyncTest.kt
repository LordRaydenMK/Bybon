package dev.sanastasov.bybon.bodyweight.data

import dev.sanastasov.bybon.account.domain.UserId
import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.bodyweight.domain.CloudWeight
import dev.sanastasov.bybon.bodyweight.domain.WeightBackupNotice
import dev.sanastasov.bybon.bodyweight.domain.WeightConflict
import dev.sanastasov.bybon.bodyweight.domain.WeightConflictChoice
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
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BodyWeightSyncTest {

    private val jan1 = LocalDate.of(2026, 1, 1)
    private val feb1 = LocalDate.of(2026, 2, 1)

    @Test
    fun `a stored conflict is returned without reading or uploading`() = runTest {
        val syncDao = FakeWeightSyncDao()
        val cloud = FakeWeightCloud()
        syncDao.upsertState(
            state(conflictKind = WeightSyncStateEntity.ACCOUNT).copy(
                conflictSignedInUid = "uid",
                conflictPreviousUid = "old",
            ),
        )
        val repository = repository(syncDao, cloud)
        repository.insert(entry(jan1, 6500))

        val result = repository.sync(WeightSyncCommand.Pull(UserId("uid"), WeightSyncStart.SignIn))

        val conflict = (result as WeightSyncResult.NeedsChoice).conflict as WeightConflict.DifferentAccount
        assert(conflict.previous == UserId("old"))
        assert(cloud.reads == 0)
        assert(cloud.writes.isEmpty())
        assert(repository.backupStatus().first().pending)
        assert(repository.backupStatus().first().notice == WeightBackupNotice.Conflict(conflict))
    }

    @Test
    fun `an empty cloud on sign-in uploads the phone`() = runTest {
        val syncDao = FakeWeightSyncDao()
        val cloud = FakeWeightCloud()
        val repository = repository(syncDao, cloud)
        repository.insert(entry(jan1, 6500))

        val result = repository.sync(WeightSyncCommand.Pull(UserId("uid"), WeightSyncStart.SignIn))

        assert(result == WeightSyncResult.Completed)
        assert(cloud.writes.single().single().date == jan1)
        assert(!cloud.writes.single().single().deleted)
        assert(syncDao.observeOps().first().isEmpty())
        assert(syncDao.observeState().first()?.lastSyncedUid == "uid")
        assert(syncDao.observeState().first()?.conflictKind == null)
        assert(repository.backupStatus().first().lastSuccess == Instant.ofEpochMilli(1_000))
    }

    @Test
    fun `a changed weight stores the clash and leaves the phone`() = runTest {
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

        val result = repository.sync(WeightSyncCommand.Pull(UserId("uid"), WeightSyncStart.SignIn))

        val clash = (
            (result as WeightSyncResult.NeedsChoice).conflict as WeightConflict.OverlappingDates
            ).dates.single()
        assert(clash.onThisPhone.value == 6500)
        assert((clash.inBackup as CloudWeight.Value).weight.value == 6600)
        assert(cloud.writes.isEmpty())
        assert(repository.entries().first().single().weight.value == 6500)
        assert(
            repository.backupStatus().first().notice ==
                WeightBackupNotice.Conflict(result.conflict),
        )
    }

    @Test
    fun `an incremental pull reads only newer documents`() = runTest {
        val syncDao = FakeWeightSyncDao()
        val cloud = FakeWeightCloud()
        syncDao.upsertState(state(cursorMillis = 10, lastSyncedUid = "uid"))
        val repository = repository(syncDao, cloud)

        repository.sync(WeightSyncCommand.Pull(UserId("uid"), WeightSyncStart.Incremental))

        assert(cloud.reads == 0)
        assert(cloud.updatedReads == 1)
    }

    @Test
    fun `a remote miss leaves the pending op`() = runTest {
        val cloud = FakeWeightCloud()
        cloud.fail = true
        val repository = repository(FakeWeightSyncDao(), cloud)
        repository.insert(entry(jan1, 6500))

        val result = repository.sync(WeightSyncCommand.Pull(UserId("uid"), WeightSyncStart.SignIn))

        assert(result == WeightSyncResult.Unavailable)
        assert(repository.backupStatus().first().pending)
        assert(repository.entries().first().single().weight.value == 6500)
    }

    @Test
    fun `resolve throws when nothing is stored`() = runTest {
        val repository = repository(FakeWeightSyncDao(), FakeWeightCloud())

        val error = assertFailsWith<IllegalStateException> {
            repository.sync(WeightSyncCommand.Resolve(WeightConflictChoice.KeepThisPhone))
        }

        assert(error.message == "No weight conflict to resolve")
    }

    @Test
    fun `use the backup replaces the phone and records the summary`() = runTest {
        val syncDao = FakeWeightSyncDao()
        val cloud = FakeWeightCloud()
        syncDao.upsertState(
            state(conflictKind = WeightSyncStateEntity.ACCOUNT).copy(
                conflictSignedInUid = "uid",
                conflictPreviousUid = "old",
            ),
        )
        val repository = repository(syncDao, cloud)
        repository.insert(entry(jan1, 6500))
        cloud.all = listOf(
            RemoteWeight(
                date = feb1,
                weight = BodyWeight(7000),
                deleted = false,
                updatedAtMillis = 9,
            ),
        )

        val result = repository.sync(
            WeightSyncCommand.Resolve(WeightConflictChoice.Cloud.UseBackup(UserId("uid"))),
        )

        assert(result == WeightSyncResult.Completed)
        val stored = repository.entries().first().single()
        assert(stored.date == feb1)
        assert(stored.weight.value == 7000)
        assert(syncDao.observeOps().first().isEmpty())
        val status = repository.backupStatus().first()
        val imported = status.notice as WeightBackupNotice.Imported
        assert(imported.summary.count == 1)
        assert(imported.summary.span.earliest == feb1)
    }

    @Test
    fun `replace the backup writes the phone and tombstones cloud-only dates`() = runTest {
        val syncDao = FakeWeightSyncDao()
        val cloud = FakeWeightCloud()
        syncDao.upsertState(state(conflictKind = WeightSyncStateEntity.DATES))
        syncDao.insertClashes(
            listOf(
                WeightSyncClashEntity(
                    date = jan1,
                    phoneWeight = 6500,
                    backupWeight = 7000,
                    backupDeleted = false,
                ),
            ),
        )
        val repository = repository(syncDao, cloud)
        repository.insert(entry(jan1, 6500))
        cloud.all = listOf(
            RemoteWeight(
                date = feb1,
                weight = BodyWeight(7000),
                deleted = false,
                updatedAtMillis = 4,
            ),
        )

        val result = repository.sync(
            WeightSyncCommand.Resolve(WeightConflictChoice.Cloud.ReplaceBackup(UserId("uid"))),
        )

        assert(result == WeightSyncResult.Completed)
        val writes = cloud.writes.single()
        assert(writes.any { it.date == jan1 && it.weight == 6500 && !it.deleted })
        assert(writes.any { it.date == feb1 && it.deleted })
        assert(repository.backupStatus().first().notice == null)
        assert(repository.entries().first().single().date == jan1)
    }

    @Test
    fun `keep this phone clears the conflict and uploads nothing`() = runTest {
        val syncDao = FakeWeightSyncDao()
        val cloud = FakeWeightCloud()
        syncDao.upsertState(
            state(conflictKind = WeightSyncStateEntity.ACCOUNT).copy(
                conflictSignedInUid = "uid",
                conflictPreviousUid = "old",
            ),
        )
        val repository = repository(syncDao, cloud)
        repository.insert(entry(jan1, 6500))

        val result = repository.sync(WeightSyncCommand.Resolve(WeightConflictChoice.KeepThisPhone))

        assert(result == WeightSyncResult.Completed)
        assert(cloud.writes.isEmpty())
        assert(syncDao.observeOps().first().isEmpty())
        assert(repository.entries().first().single().date == jan1)
        assert(repository.backupStatus().first().notice == null)
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

        val imported = repository.backupStatus().first().notice as WeightBackupNotice.Imported
        assert(imported.summary.count == 1)
        repository.dismissSummary()

        assert(repository.backupStatus().first().notice == null)
    }

    private fun repository(syncDao: WeightSyncDao, cloud: WeightCloud) = BodyWeightRepositoryImpl(
        FakeWeightDao(),
        EmptyDietPhaseDao(),
        syncDao,
        cloud,
        Clock.fixed(Instant.ofEpochMilli(1_000), ZoneOffset.UTC),
    )

    private fun entry(date: LocalDate, weight: Int) = BodyWeightEntry(date, BodyWeight(weight))

    private fun state(
        cursorMillis: Long? = null,
        lastSyncedUid: String? = null,
        conflictKind: String? = null,
    ) = WeightSyncStateEntity(
        lastSyncedUid = lastSyncedUid,
        cursorMillis = cursorMillis,
        lastSuccessMillis = null,
        conflictKind = conflictKind,
        conflictSignedInUid = null,
        conflictPreviousUid = null,
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
