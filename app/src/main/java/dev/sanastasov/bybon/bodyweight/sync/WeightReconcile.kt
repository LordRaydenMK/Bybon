package dev.sanastasov.bybon.bodyweight.sync

import dev.sanastasov.bybon.account.domain.UserId
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.bodyweight.domain.WeightConflict
import dev.sanastasov.bybon.bodyweight.domain.WeightDateSpan
import dev.sanastasov.bybon.bodyweight.domain.WeightImportSummary
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncOp
import java.time.LocalDate

data class WeightUpload(
    val date: LocalDate,
    val weight: Int?,
    val deleted: Boolean,
)

data class WeightSyncInput(
    val local: List<BodyWeightEntry>,
    val remote: List<RemoteWeight>,
    val pending: List<WeightSyncOp>,
    val fullSnapshot: Boolean,
    val signedInUid: UserId,
    val lastSyncedUid: UserId?,
    val conflictPending: Boolean,
)

sealed interface WeightSyncPlan {
    data object Blocked : WeightSyncPlan

    data class Ask(
        val conflict: WeightConflict,
    ) : WeightSyncPlan

    data class Apply(
        val replaceLocal: List<BodyWeightEntry>?,
        val addLocal: List<BodyWeightEntry>,
        val uploads: List<WeightUpload>,
        val uploadAllLocal: Boolean,
        val summary: WeightImportSummary?,
        val cursorMillis: Long?,
    ) : WeightSyncPlan
}

fun reconcileWeights(input: WeightSyncInput): WeightSyncPlan = when {
    input.conflictPending -> WeightSyncPlan.Blocked

    differentAccount(input) -> WeightSyncPlan.Ask(
        WeightConflict.DifferentAccount(
            signedIn = input.signedInUid,
            previous = checkNotNull(input.lastSyncedUid),
        ),
    )

    else -> askOrApply(input)
}

private fun differentAccount(input: WeightSyncInput): Boolean {
    val previous = input.lastSyncedUid
    return previous != null && previous != input.signedInUid && input.local.isNotEmpty()
}

private fun askOrApply(input: WeightSyncInput): WeightSyncPlan =
    if (hasDateClash(input.local, input.remote)) {
        WeightSyncPlan.Ask(WeightConflict.OverlappingDates)
    } else {
        planChanges(input)
    }

internal fun List<BodyWeightEntry>.toImportSummary(): WeightImportSummary? {
    if (isEmpty()) return null
    val dates = map { it.date }
    return WeightImportSummary(
        count = size,
        span = WeightDateSpan(dates.min(), dates.max()),
    )
}

private fun planChanges(input: WeightSyncInput): WeightSyncPlan.Apply {
    val live = liveEntries(input.remote)
    val pendingDeletes = input.pending
        .filterIsInstance<WeightSyncOp.Delete>()
        .map { it.date }
        .toSet()
    val cursor = input.remote.maxOfOrNull { it.updatedAtMillis }
    val added = adds(input.local, input.remote, pendingDeletes)
    return when {
        input.fullSnapshot && live.isEmpty() -> WeightSyncPlan.Apply(
            replaceLocal = null,
            addLocal = emptyList(),
            uploads = input.pending.filterIsInstance<WeightSyncOp.Delete>().toUploads(),
            uploadAllLocal = input.local.isNotEmpty(),
            summary = null,
            cursorMillis = cursor,
        )

        input.fullSnapshot && input.local.isEmpty() -> WeightSyncPlan.Apply(
            replaceLocal = if (pendingDeletes.isEmpty()) added else null,
            addLocal = if (pendingDeletes.isEmpty()) emptyList() else added,
            uploads = input.pending.toUploads(),
            uploadAllLocal = false,
            summary = added.toImportSummary(),
            cursorMillis = cursor,
        )

        else -> WeightSyncPlan.Apply(
            replaceLocal = null,
            addLocal = added,
            uploads = input.pending.toUploads(),
            uploadAllLocal = false,
            summary = added.toImportSummary(),
            cursorMillis = cursor,
        )
    }
}

private fun hasDateClash(local: List<BodyWeightEntry>, remote: List<RemoteWeight>): Boolean {
    val byDate = remote.associateBy { it.date }
    return local.any { entry ->
        val doc = byDate[entry.date] ?: return@any false
        val backup = doc.weight
        doc.deleted || (backup != null && backup != entry.weight)
    }
}

private fun liveEntries(remote: List<RemoteWeight>): List<BodyWeightEntry> =
    remote.mapNotNull { doc ->
        val weight = doc.weight
        if (doc.deleted || weight == null) null else BodyWeightEntry(doc.date, weight)
    }

private fun adds(
    local: List<BodyWeightEntry>,
    remote: List<RemoteWeight>,
    pendingDeletes: Set<LocalDate>,
): List<BodyWeightEntry> {
    val localDates = local.map { it.date }.toSet()
    return liveEntries(remote).filter { it.date !in localDates && it.date !in pendingDeletes }
}

private fun List<WeightSyncOp>.toUploads(): List<WeightUpload> = map { op ->
    when (op) {
        is WeightSyncOp.Upsert -> WeightUpload(
            date = op.date,
            weight = op.weight.value,
            deleted = false,
        )

        is WeightSyncOp.Delete -> WeightUpload(
            date = op.date,
            weight = null,
            deleted = true,
        )
    }
}
