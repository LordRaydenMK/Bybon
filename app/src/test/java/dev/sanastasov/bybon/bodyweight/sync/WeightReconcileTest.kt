package dev.sanastasov.bybon.bodyweight.sync

import dev.sanastasov.bybon.account.domain.UserId
import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.bodyweight.domain.WeightConflict
import dev.sanastasov.bybon.bodyweight.domain.WeightDateSpan
import dev.sanastasov.bybon.bodyweight.domain.WeightImportSummary
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncOp
import java.time.Instant
import java.time.LocalDate
import org.junit.Test

class WeightReconcileTest {

    private val jan1 = LocalDate.of(2026, 1, 1)
    private val feb1 = LocalDate.of(2026, 2, 1)

    @Test
    fun `empty phone restores live cloud weights and records a summary`() {
        val plan = reconcileWeights(
            input(
                remote = listOf(
                    remote(feb1, 6600, updatedAt = 2),
                    remote(jan1, 6500, updatedAt = 1),
                ),
            ),
        )

        val apply = plan as WeightSyncPlan.Apply
        assert(apply.replaceLocal?.map { it.date }?.toSet() == setOf(jan1, feb1))
        assert(apply.addLocal.isEmpty())
        assert(!apply.uploadAllLocal)
        assert(apply.uploads.isEmpty())
        assert(apply.summary == WeightImportSummary(2, WeightDateSpan(jan1, feb1)))
    }

    @Test
    fun `empty cloud on the first uid uploads every local entry`() {
        val plan = reconcileWeights(
            input(local = listOf(entry(jan1, 6500))),
        )

        val apply = plan as WeightSyncPlan.Apply
        assert(apply.uploadAllLocal)
        assert(apply.summary == null)
        assert(apply.replaceLocal == null)
    }

    @Test
    fun `a different uid is a conflict even when that cloud is empty`() {
        val plan = reconcileWeights(
            input(
                local = listOf(entry(jan1, 6500)),
                signedInUid = UserId("other"),
                lastSyncedUid = UserId("uid"),
            ),
        )

        val conflict = (plan as WeightSyncPlan.Conflict).conflict as WeightConflict.DifferentAccount
        assert(conflict.signedIn == UserId("other"))
        assert(conflict.previous == UserId("uid"))
    }

    @Test
    fun `a different uid with an empty phone restores that cloud`() {
        val plan = reconcileWeights(
            input(
                remote = listOf(remote(jan1, 6500)),
                signedInUid = UserId("other"),
                lastSyncedUid = UserId("uid"),
            ),
        )

        val apply = plan as WeightSyncPlan.Apply
        assert(apply.replaceLocal?.single()?.weight?.value == 6500)
        assert(apply.summary?.count == 1)
    }

    @Test
    fun `matching shared dates add only the extra live dates`() {
        val plan = reconcileWeights(
            input(
                local = listOf(entry(jan1, 6500)),
                remote = listOf(
                    remote(jan1, 6500, updatedAt = 1),
                    remote(feb1, 6600, updatedAt = 2),
                ),
                lastSyncedUid = UserId("uid"),
            ),
        )

        val apply = plan as WeightSyncPlan.Apply
        assert(apply.addLocal.map { it.date } == listOf(feb1))
        assert(apply.replaceLocal == null)
        assert(!apply.uploadAllLocal)
        assert(apply.uploads.isEmpty())
        assert(apply.summary == WeightImportSummary(1, WeightDateSpan(feb1, feb1)))
    }

    @Test
    fun `a changed cloud weight is a conflict`() {
        val plan = reconcileWeights(
            input(
                local = listOf(entry(jan1, 6500)),
                remote = listOf(remote(jan1, 6550)),
                pending = listOf(WeightSyncOp.Upsert(jan1, BodyWeight(6500))),
                lastSyncedUid = UserId("uid"),
            ),
        )

        assert((plan as WeightSyncPlan.Conflict).conflict == WeightConflict.OverlappingDates)
    }

    @Test
    fun `a tombstone for a phone weight is a conflict`() {
        val plan = reconcileWeights(
            input(
                local = listOf(entry(jan1, 6500)),
                remote = listOf(remote(jan1, 6500, deleted = true)),
                lastSyncedUid = UserId("uid"),
            ),
        )

        assert((plan as WeightSyncPlan.Conflict).conflict == WeightConflict.OverlappingDates)
    }

    @Test
    fun `a pending delete is not restored from a live cloud document`() {
        val plan = reconcileWeights(
            input(
                remote = listOf(remote(jan1, 6500), remote(feb1, 6600)),
                pending = listOf(WeightSyncOp.Delete(jan1)),
                lastSyncedUid = UserId("uid"),
            ),
        )

        val apply = plan as WeightSyncPlan.Apply
        assert(apply.replaceLocal == null)
        assert(apply.addLocal.map { it.date } == listOf(feb1))
        assert(apply.uploads.single().deleted)
        assert(apply.uploads.single().date == jan1)
    }

    @Test
    fun `parse skips invalid documents and keeps tombstones`() {
        val updatedAt = Instant.ofEpochMilli(10)
        val parsed = parseWeightDocument("2026-01-01", 6500, false, updatedAt)
        assert(parsed?.weight == BodyWeight(6500))
        assert(parseWeightDocument("nope", 6500, false, updatedAt) == null)
        assert(parseWeightDocument("2026-01-01", 6501, false, updatedAt) == null)
        assert(parseWeightDocument("2026-01-01", 6500, false, null) == null)
        assert(parseWeightDocument("2026-01-01", null, true, updatedAt)?.deleted == true)
    }

    private fun entry(date: LocalDate, weight: Int) = BodyWeightEntry(date, BodyWeight(weight))

    private fun remote(
        date: LocalDate,
        weight: Int,
        deleted: Boolean = false,
        updatedAt: Long = 1,
    ) = RemoteWeight(
        date = date,
        weight = if (deleted) null else BodyWeight(weight),
        deleted = deleted,
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )

    private fun input(
        local: List<BodyWeightEntry> = emptyList(),
        remote: List<RemoteWeight> = emptyList(),
        pending: List<WeightSyncOp> = emptyList(),
        fullSnapshot: Boolean = true,
        signedInUid: UserId = UserId("uid"),
        lastSyncedUid: UserId? = null,
    ) = WeightSyncInput(
        local = local,
        remote = remote,
        pending = pending,
        fullSnapshot = fullSnapshot,
        signedInUid = signedInUid,
        lastSyncedUid = lastSyncedUid,
    )
}
