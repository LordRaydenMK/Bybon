package dev.sanastasov.bybon.bodyweight.data

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.bodyweight.domain.CloudWeight
import dev.sanastasov.bybon.bodyweight.domain.WeightBackupStatus
import dev.sanastasov.bybon.bodyweight.domain.WeightConflict
import dev.sanastasov.bybon.bodyweight.domain.WeightConflictChoice
import dev.sanastasov.bybon.bodyweight.domain.WeightDateClash
import dev.sanastasov.bybon.bodyweight.domain.WeightImportSummary
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncCommand
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncOp
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncOpKind
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncResult
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncStart
import dev.sanastasov.bybon.bodyweight.sync.RemoteWeight
import dev.sanastasov.bybon.bodyweight.sync.WeightCloud
import dev.sanastasov.bybon.bodyweight.sync.WeightCloudException
import dev.sanastasov.bybon.bodyweight.sync.WeightSyncInput
import dev.sanastasov.bybon.bodyweight.sync.WeightSyncPlan
import dev.sanastasov.bybon.bodyweight.sync.WeightUpload
import dev.sanastasov.bybon.bodyweight.sync.isSyncableWeight
import dev.sanastasov.bybon.bodyweight.sync.reconcileWeights
import dev.sanastasov.bybon.bodyweight.sync.toImportSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Suppress("TooManyFunctions")
internal class WeightSyncEngine(
    private val weights: WeightDao,
    private val syncDao: WeightSyncDao,
    private val cloud: WeightCloud,
    private val databaseName: String,
    private val clock: () -> Long,
) {
    private val gate = Mutex()

    fun status(): Flow<WeightBackupStatus> = combine(
        syncDao.observeOps(),
        syncDao.observeState(databaseName),
        syncDao.observeClashes(databaseName),
    ) { ops, state, clashes ->
        WeightBackupStatus(
            pending = ops.isNotEmpty(),
            lastSuccessMillis = state?.lastSuccessMillis,
            conflict = conflictOf(state, clashes),
            summary = state.toSummary(),
        )
    }

    suspend fun sync(command: WeightSyncCommand): WeightSyncResult = gate.withLock {
        when (command) {
            is WeightSyncCommand.Pull -> pull(command)
            is WeightSyncCommand.Resolve -> resolve(command.choice)
        }
    }

    suspend fun dismissSummary() {
        gate.withLock {
            val current = syncDao.state(databaseName) ?: return@withLock
            syncDao.upsertState(
                current.copy(
                    summaryCount = null,
                    summaryEarliest = null,
                    summaryLatest = null,
                ),
            )
        }
    }

    private suspend fun pull(command: WeightSyncCommand.Pull): WeightSyncResult {
        val state = syncDao.state(databaseName)
        val stored = conflictOf(state, syncDao.clashes(databaseName))
        return if (stored != null) {
            WeightSyncResult.NeedsChoice(stored)
        } else {
            pullRemote(command, state)
        }
    }

    private suspend fun pullRemote(
        command: WeightSyncCommand.Pull,
        state: WeightSyncStateEntity?,
    ): WeightSyncResult {
        val uidChanged = state?.lastSyncedUid != null && state.lastSyncedUid != command.uid
        val full = command.start == WeightSyncStart.SignIn ||
            state?.cursorMillis == null ||
            uidChanged
        val remote = cloud.readOrNull(full, command.uid, state?.cursorMillis)
        val local = localEntries()
        return if (remote == null) {
            WeightSyncResult.Unavailable
        } else {
            applyPlan(command.uid, local, state, remote, full)
        }
    }

    private suspend fun applyPlan(
        uid: String,
        local: List<BodyWeightEntry>,
        state: WeightSyncStateEntity?,
        remote: List<RemoteWeight>,
        full: Boolean,
    ): WeightSyncResult {
        val plan = reconcileWeights(
            WeightSyncInput(
                local = local,
                remote = remote,
                pending = pendingOps(),
                fullSnapshot = full,
                signedInUid = uid,
                lastSyncedUid = state?.lastSyncedUid,
                conflictPending = false,
            ),
        )
        return when (plan) {
            WeightSyncPlan.Blocked -> error("Weight sync blocked without a stored conflict")

            is WeightSyncPlan.Ask -> {
                storeConflict(state, plan.conflict)
                WeightSyncResult.NeedsChoice(plan.conflict)
            }

            is WeightSyncPlan.Apply -> writePlan(uid, local, state, plan)
        }
    }

    private suspend fun writePlan(
        uid: String,
        local: List<BodyWeightEntry>,
        state: WeightSyncStateEntity?,
        plan: WeightSyncPlan.Apply,
    ): WeightSyncResult {
        plan.replaceLocal?.let { entries -> replaceWeights(entries) }
        if (plan.addLocal.isNotEmpty()) weights.insertWeights(plan.addLocal.map { it.toEntity() })
        val uploads = buildList {
            if (plan.uploadAllLocal) addAll(local.map { it.toUpload() })
            addAll(plan.uploads)
        }
        return if (cloud.writeOrFalse(uid, uploads)) {
            if (plan.uploadAllLocal) {
                syncDao.clearOps()
            } else if (plan.uploads.isNotEmpty()) {
                syncDao.deleteOps(plan.uploads.map { it.date })
            }
            saveFinished(
                uid = uid,
                previous = state,
                cursorMillis = plan.cursorMillis,
                wrote = uploads.isNotEmpty(),
                summary = plan.summary,
                keepPreviousSummary = plan.summary == null,
            )
            WeightSyncResult.Completed
        } else {
            WeightSyncResult.Unavailable
        }
    }

    private suspend fun resolve(choice: WeightConflictChoice): WeightSyncResult {
        val state = syncDao.state(databaseName)
        if (conflictOf(state, syncDao.clashes(databaseName)) == null) {
            error("No weight conflict to resolve")
        }
        return when (choice) {
            WeightConflictChoice.KeepThisPhone -> keepPhone(state)
            is WeightConflictChoice.UseBackup -> useBackup(choice.uid, state)
            is WeightConflictChoice.ReplaceBackup -> replaceBackup(choice.uid, state)
        }
    }

    private suspend fun keepPhone(state: WeightSyncStateEntity?): WeightSyncResult {
        syncDao.clearOps()
        val current = state ?: emptyState()
        syncDao.replaceState(current.withoutConflict(), emptyList())
        return WeightSyncResult.Completed
    }

    private suspend fun useBackup(uid: String, previous: WeightSyncStateEntity?): WeightSyncResult {
        val remote = cloud.readOrNull(full = true, uid = uid, cursor = null)
        return if (remote == null) {
            WeightSyncResult.Unavailable
        } else {
            val live = remote.toLiveEntries()
            replaceWeights(live)
            syncDao.clearOps()
            saveFinished(
                uid = uid,
                previous = previous,
                cursorMillis = remote.maxOfOrNull { it.updatedAtMillis },
                wrote = false,
                summary = live.toImportSummary(),
                keepPreviousSummary = false,
            )
            WeightSyncResult.Completed
        }
    }

    private suspend fun replaceBackup(
        uid: String,
        previous: WeightSyncStateEntity?,
    ): WeightSyncResult {
        val remote = cloud.readOrNull(full = true, uid = uid, cursor = null)
        return if (remote == null) {
            WeightSyncResult.Unavailable
        } else {
            replaceCloud(uid, previous, remote)
        }
    }

    private suspend fun replaceCloud(
        uid: String,
        previous: WeightSyncStateEntity?,
        remote: List<RemoteWeight>,
    ): WeightSyncResult {
        val local = localEntries()
        val localDates = local.map { it.date }.toSet()
        val uploads = local.map { it.toUpload() } + remote
            .filter { !it.deleted && it.date !in localDates }
            .map { WeightUpload(it.date, weight = null, deleted = true) }
        return if (cloud.writeOrFalse(uid, uploads)) {
            syncDao.clearOps()
            saveFinished(
                uid = uid,
                previous = previous,
                cursorMillis = remote.maxOfOrNull { it.updatedAtMillis },
                wrote = uploads.isNotEmpty(),
                summary = null,
                keepPreviousSummary = false,
            )
            WeightSyncResult.Completed
        } else {
            WeightSyncResult.Unavailable
        }
    }

    private suspend fun replaceWeights(entries: List<BodyWeightEntry>) {
        weights.deleteAllWeights()
        if (entries.isNotEmpty()) weights.insertWeights(entries.map { it.toEntity() })
    }

    private suspend fun localEntries(): List<BodyWeightEntry> =
        weights.weightEntries().first().map { BodyWeightEntry(it.date, BodyWeight(it.weight)) }

    private suspend fun pendingOps(): List<WeightSyncOp> = syncDao.ops().map { it.toOp() }

    private suspend fun storeConflict(previous: WeightSyncStateEntity?, conflict: WeightConflict) {
        val base = previous ?: emptyState()
        syncDao.replaceState(base.withConflict(conflict), conflict.toClashes())
    }

    private suspend fun saveFinished(
        uid: String,
        previous: WeightSyncStateEntity?,
        cursorMillis: Long?,
        wrote: Boolean,
        summary: WeightImportSummary?,
        keepPreviousSummary: Boolean,
    ) {
        val now = clock()
        val cursor = listOfNotNull(
            previous?.cursorMillis,
            cursorMillis,
            if (wrote || cursorMillis == null) now else null,
        ).max()
        val pendingLeft = syncDao.ops().isNotEmpty()
        val resolved = when {
            summary != null -> summary
            keepPreviousSummary -> previous.toSummary()
            else -> null
        }
        syncDao.replaceState(
            WeightSyncStateEntity(
                databaseName = databaseName,
                lastSyncedUid = uid,
                cursorMillis = cursor,
                lastSuccessMillis = if (pendingLeft) previous?.lastSuccessMillis else now,
                conflictKind = null,
                conflictSignedInUid = null,
                conflictPreviousUid = null,
                summaryCount = resolved?.count,
                summaryEarliest = resolved?.earliest,
                summaryLatest = resolved?.latest,
            ),
            emptyList(),
        )
    }

    private fun emptyState(): WeightSyncStateEntity = WeightSyncStateEntity(
        databaseName = databaseName,
        lastSyncedUid = null,
        cursorMillis = null,
        lastSuccessMillis = null,
        conflictKind = null,
        conflictSignedInUid = null,
        conflictPreviousUid = null,
        summaryCount = null,
        summaryEarliest = null,
        summaryLatest = null,
    )

    private fun WeightSyncStateEntity.withConflict(
        conflict: WeightConflict,
    ): WeightSyncStateEntity = when (conflict) {
        is WeightConflict.DifferentAccount -> copy(
            conflictKind = WeightSyncStateEntity.ACCOUNT,
            conflictSignedInUid = conflict.signedInUid,
            conflictPreviousUid = conflict.previousUid,
            summaryCount = null,
            summaryEarliest = null,
            summaryLatest = null,
        )

        is WeightConflict.OverlappingDates -> copy(
            conflictKind = WeightSyncStateEntity.DATES,
            conflictSignedInUid = null,
            conflictPreviousUid = null,
            summaryCount = null,
            summaryEarliest = null,
            summaryLatest = null,
        )
    }

    private fun WeightSyncStateEntity.withoutConflict(): WeightSyncStateEntity = copy(
        conflictKind = null,
        conflictSignedInUid = null,
        conflictPreviousUid = null,
        summaryCount = null,
        summaryEarliest = null,
        summaryLatest = null,
    )

    private fun WeightConflict.toClashes(): List<WeightSyncClashEntity> = when (this) {
        is WeightConflict.DifferentAccount -> emptyList()

        is WeightConflict.OverlappingDates -> dates.map { clash ->
            WeightSyncClashEntity(
                databaseName = databaseName,
                date = clash.date,
                phoneWeight = clash.onThisPhone.value,
                backupWeight = (clash.inBackup as? CloudWeight.Value)?.weight?.value,
                backupDeleted = clash.inBackup is CloudWeight.Deleted,
            )
        }
    }
}

private fun conflictOf(
    state: WeightSyncStateEntity?,
    clashes: List<WeightSyncClashEntity>,
): WeightConflict? {
    val kind = state?.conflictKind ?: return null
    return when (kind) {
        WeightSyncStateEntity.ACCOUNT -> WeightConflict.DifferentAccount(
            signedInUid = state.conflictSignedInUid
                ?: error("Stored account conflict is missing the signed-in account"),
            previousUid = state.conflictPreviousUid
                ?: error("Stored account conflict is missing the previous account"),
        )

        WeightSyncStateEntity.DATES -> {
            if (clashes.isEmpty()) error("Stored date conflict has no dates")
            WeightConflict.OverlappingDates(clashes.map { it.toClash() }.sortedBy { it.date })
        }

        else -> error("Unknown weight conflict '$kind'")
    }
}

private fun WeightSyncClashEntity.toClash(): WeightDateClash {
    val backup = if (backupDeleted) {
        CloudWeight.Deleted
    } else {
        CloudWeight.Value(BodyWeight(checkNotNull(backupWeight)))
    }
    return WeightDateClash(date, BodyWeight(phoneWeight), backup)
}

private fun WeightSyncStateEntity?.toSummary(): WeightImportSummary? {
    val count = this?.summaryCount
    val earliest = this?.summaryEarliest
    val latest = this?.summaryLatest
    return if (count == null || earliest == null || latest == null) {
        null
    } else if (count < 1) {
        null
    } else {
        WeightImportSummary(count, earliest, latest)
    }
}

private fun WeightSyncOpEntity.toOp(): WeightSyncOp = WeightSyncOp(
    date = date,
    kind = if (kind == WeightSyncOpEntity.DELETE) {
        WeightSyncOpKind.Delete
    } else {
        WeightSyncOpKind.Upsert
    },
    weight = weight,
)

private fun BodyWeightEntry.toEntity(): WeightEntryEntity = WeightEntryEntity(date, weight.value)

private fun BodyWeightEntry.toUpload(): WeightUpload = WeightUpload(
    date = date,
    weight = weight.value,
    deleted = false,
)

private fun List<RemoteWeight>.toLiveEntries(): List<BodyWeightEntry> =
    filter { !it.deleted && it.weight.isSyncableWeight() }
        .map { BodyWeightEntry(it.date, BodyWeight(it.weight)) }

private suspend fun WeightCloud.readOrNull(
    full: Boolean,
    uid: String,
    cursor: Long?,
): List<RemoteWeight>? = try {
    if (full || cursor == null) readAll(uid) else readUpdatedAfter(uid, cursor)
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: WeightCloudException) {
    null
}

private suspend fun WeightCloud.writeOrFalse(uid: String, documents: List<WeightUpload>): Boolean =
    try {
        if (documents.isNotEmpty()) write(uid, documents)
        true
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: WeightCloudException) {
        false
    }
