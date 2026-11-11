package dev.sanastasov.bybon.bodyweight.sync

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.bodyweight.domain.CloudWeight
import dev.sanastasov.bybon.bodyweight.domain.WeightConflict
import dev.sanastasov.bybon.bodyweight.domain.WeightDateClash
import dev.sanastasov.bybon.bodyweight.domain.WeightImportSummary
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncOp
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncOpKind
import java.time.LocalDate

data class RemoteWeight(
    val date: LocalDate,
    val weight: Int,
    val deleted: Boolean,
    val updatedAtMillis: Long,
)

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
    val signedInUid: String,
    val lastSyncedUid: String?,
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
            signedInUid = input.signedInUid,
            previousUid = checkNotNull(input.lastSyncedUid),
        ),
    )

    else -> askOrApply(input)
}

private fun differentAccount(input: WeightSyncInput): Boolean {
    val previous = input.lastSyncedUid
    return previous != null && previous != input.signedInUid && input.local.isNotEmpty()
}

private fun askOrApply(input: WeightSyncInput): WeightSyncPlan {
    val dates = clashes(input.local, input.remote)
    return if (dates.isEmpty()) {
        planChanges(input)
    } else {
        WeightSyncPlan.Ask(WeightConflict.OverlappingDates(dates))
    }
}

internal fun List<BodyWeightEntry>.toImportSummary(): WeightImportSummary? {
    if (isEmpty()) return null
    val dates = map { it.date }
    return WeightImportSummary(
        count = size,
        earliest = dates.min(),
        latest = dates.max(),
    )
}

internal fun Int.isSyncableWeight(): Boolean = this > 0 && this % 5 == 0 && this < 50_000

private fun planChanges(input: WeightSyncInput): WeightSyncPlan.Apply {
    val live = liveEntries(input.remote)
    val pendingDeletes = input.pending
        .filter { it.kind == WeightSyncOpKind.Delete }
        .map { it.date }
        .toSet()
    val cursor = input.remote.maxOfOrNull { it.updatedAtMillis }
    val added = adds(input.local, input.remote, pendingDeletes)
    return when {
        input.fullSnapshot && live.isEmpty() -> WeightSyncPlan.Apply(
            replaceLocal = null,
            addLocal = emptyList(),
            uploads = input.pending.filter { it.kind == WeightSyncOpKind.Delete }.toUploads(),
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

private fun clashes(
    local: List<BodyWeightEntry>,
    remote: List<RemoteWeight>,
): List<WeightDateClash> {
    val byDate = remote.associateBy { it.date }
    return local.mapNotNull { entry ->
        val doc = byDate[entry.date] ?: return@mapNotNull null
        when {
            doc.deleted -> WeightDateClash(entry.date, entry.weight, CloudWeight.Deleted)

            doc.weight != entry.weight.value -> WeightDateClash(
                date = entry.date,
                onThisPhone = entry.weight,
                inBackup = CloudWeight.Value(BodyWeight(doc.weight)),
            )

            else -> null
        }
    }.sortedBy { it.date }
}

private fun liveEntries(remote: List<RemoteWeight>): List<BodyWeightEntry> =
    remote.filter { !it.deleted && it.weight.isSyncableWeight() }
        .map { BodyWeightEntry(it.date, BodyWeight(it.weight)) }

private fun adds(
    local: List<BodyWeightEntry>,
    remote: List<RemoteWeight>,
    pendingDeletes: Set<LocalDate>,
): List<BodyWeightEntry> {
    val localDates = local.map { it.date }.toSet()
    return liveEntries(remote).filter { it.date !in localDates && it.date !in pendingDeletes }
}

private fun List<WeightSyncOp>.toUploads(): List<WeightUpload> = map { op ->
    when (op.kind) {
        WeightSyncOpKind.Upsert -> WeightUpload(
            date = op.date,
            weight = op.weight,
            deleted = false,
        )

        WeightSyncOpKind.Delete -> WeightUpload(
            date = op.date,
            weight = null,
            deleted = true,
        )
    }
}
