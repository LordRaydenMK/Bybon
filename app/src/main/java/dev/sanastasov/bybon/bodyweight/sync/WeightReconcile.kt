package dev.sanastasov.bybon.bodyweight.sync

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
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

data class WeightImportSummary(
    val count: Int,
    val earliest: LocalDate,
    val latest: LocalDate,
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

    data object Ask : WeightSyncPlan

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

    input.lastSyncedUid != null &&
        input.lastSyncedUid != input.signedInUid &&
        input.local.isNotEmpty() -> WeightSyncPlan.Ask

    wouldChangePhone(input.local, input.remote) -> WeightSyncPlan.Ask

    else -> planChanges(input)
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

private fun wouldChangePhone(local: List<BodyWeightEntry>, remote: List<RemoteWeight>): Boolean {
    val byDate = remote.associateBy { it.date }
    return local.any { entry ->
        val doc = byDate[entry.date] ?: return@any false
        doc.deleted || doc.weight != entry.weight.value
    }
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
