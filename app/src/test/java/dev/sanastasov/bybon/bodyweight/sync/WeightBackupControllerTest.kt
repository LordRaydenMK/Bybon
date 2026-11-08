package dev.sanastasov.bybon.bodyweight.sync

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.bodyweight.FakeBodyWeightRepository
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncState
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WeightBackupControllerTest {

    private val jan1 = LocalDate.of(2026, 1, 1)
    private val feb1 = LocalDate.of(2026, 2, 1)

    @Test
    fun `an unanswered conflict does not read or upload`() = runTest {
        val repository = FakeBodyWeightRepository()
        val cloud = FakeWeightCloud()
        repository.saveSyncState(state(conflictPending = true))
        repository.insert(entry(jan1, 6500))
        val controller = controller(repository, cloud)

        controller.sync("stg", "uid", signIn = true)

        assert(cloud.reads == 0)
        assert(cloud.writes.isEmpty())
        assert(repository.pendingOps().single().date == jan1)
    }

    @Test
    fun `an empty cloud on sign-in uploads the phone`() = runTest {
        val repository = FakeBodyWeightRepository()
        val cloud = FakeWeightCloud()
        repository.insert(entry(jan1, 6500))
        val controller = controller(repository, cloud)

        controller.sync("stg", "uid", signIn = true)

        assert(cloud.writes.single().single().date == jan1)
        assert(!cloud.writes.single().single().deleted)
        assert(repository.pendingOps().isEmpty())
        assert(repository.syncState("stg")?.lastSyncedUid == "uid")
        assert(repository.syncState("stg")?.conflictPending == false)
    }

    @Test
    fun `a changed weight sets the conflict and leaves the phone`() = runTest {
        val repository = FakeBodyWeightRepository()
        val cloud = FakeWeightCloud()
        repository.insert(entry(jan1, 6500))
        cloud.all = listOf(RemoteWeight(jan1, 6600, deleted = false, updatedAtMillis = 5))
        val controller = controller(repository, cloud)

        controller.sync("stg", "uid", signIn = true)

        assert(cloud.writes.isEmpty())
        assert(repository.syncState("stg")?.conflictPending == true)
        assert(repository.entries().first().single().weight.value == 6500)
    }

    @Test
    fun `account open with a cursor reads only newer documents`() = runTest {
        val repository = FakeBodyWeightRepository()
        val cloud = FakeWeightCloud()
        repository.saveSyncState(state(cursorMillis = 10))
        val controller = controller(repository, cloud)

        controller.sync("stg", "uid", signIn = false)

        assert(cloud.reads == 0)
        assert(cloud.updatedReads == 1)
    }

    @Test
    fun `use the backup replaces the phone and keeps the session data`() = runTest {
        val repository = FakeBodyWeightRepository()
        val cloud = FakeWeightCloud()
        repository.insert(entry(jan1, 6500))
        cloud.all = listOf(RemoteWeight(feb1, 7000, deleted = false, updatedAtMillis = 9))
        val controller = controller(repository, cloud)

        controller.useBackup("stg", "uid")

        val stored = repository.entries().first().single()
        assert(stored.date == feb1)
        assert(stored.weight.value == 7000)
        assert(repository.pendingOps().isEmpty())
        assert(repository.syncState("stg")?.summaryCount == 1)
        assert(repository.syncState("stg")?.conflictPending == false)
    }

    @Test
    fun `replace the backup writes the phone and tombstones cloud-only dates`() = runTest {
        val repository = FakeBodyWeightRepository()
        val cloud = FakeWeightCloud()
        repository.insert(entry(jan1, 6500))
        cloud.all = listOf(RemoteWeight(feb1, 7000, deleted = false, updatedAtMillis = 4))
        val controller = controller(repository, cloud)

        controller.replaceBackup("stg", "uid")

        val writes = cloud.writes.single()
        assert(writes.any { it.date == jan1 && it.weight == 6500 && !it.deleted })
        assert(writes.any { it.date == feb1 && it.deleted })
        assert(repository.syncState("stg")?.conflictPending == false)
        assert(repository.entries().first().single().date == jan1)
    }

    private fun controller(repository: FakeBodyWeightRepository, cloud: FakeWeightCloud) =
        WeightBackupController(repository, cloud) { 1_000 }

    private fun entry(date: LocalDate, weight: Int) = BodyWeightEntry(date, BodyWeight(weight))

    private fun state(cursorMillis: Long? = null, conflictPending: Boolean = false) =
        WeightSyncState(
            databaseName = "stg",
            lastSyncedUid = "uid",
            cursorMillis = cursorMillis,
            lastSuccessMillis = null,
            conflictPending = conflictPending,
            summaryCount = null,
            summaryEarliest = null,
            summaryLatest = null,
        )
}

private class FakeWeightCloud : WeightCloud {
    var all: List<RemoteWeight> = emptyList()
    var updated: List<RemoteWeight> = emptyList()
    val writes = mutableListOf<List<WeightUpload>>()
    var reads = 0
    var updatedReads = 0

    override suspend fun readAll(uid: String): List<RemoteWeight> {
        reads += 1
        return all
    }

    override suspend fun readUpdatedAfter(uid: String, cursorMillis: Long): List<RemoteWeight> {
        updatedReads += 1
        return updated
    }

    override suspend fun write(uid: String, documents: List<WeightUpload>) {
        writes += documents
    }
}
