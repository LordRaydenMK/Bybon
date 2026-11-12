package dev.sanastasov.bybon.bodyweight.data

import com.google.firebase.FirebaseException
import dev.sanastasov.bybon.account.domain.UserId
import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.bodyweight.domain.WeightBackupNotice
import dev.sanastasov.bybon.bodyweight.domain.WeightBackupStatus
import dev.sanastasov.bybon.bodyweight.domain.WeightConflict
import dev.sanastasov.bybon.bodyweight.domain.WeightConflictChoice
import dev.sanastasov.bybon.bodyweight.domain.WeightDateSpan
import dev.sanastasov.bybon.bodyweight.domain.WeightImportSummary
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncCommand
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncOp
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncResult
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncStart
import dev.sanastasov.bybon.bodyweight.sync.RemoteWeight
import dev.sanastasov.bybon.bodyweight.sync.WeightCloud
import dev.sanastasov.bybon.bodyweight.sync.WeightSyncInput
import dev.sanastasov.bybon.bodyweight.sync.WeightSyncPlan
import dev.sanastasov.bybon.bodyweight.sync.WeightUpload
import dev.sanastasov.bybon.bodyweight.sync.reconcileWeights
import dev.sanastasov.bybon.bodyweight.sync.toImportSummary
import java.io.IOException
import java.time.Clock
import java.time.Instant
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
    private val clock: Clock,
) {
    private val gate = Mutex()

    fun status(): Flow<WeightBackupStatus> = combine(
        syncDao.observeOps(),
        syncDao.observeState(),
    ) { ops, state ->
        val conflict = conflictOf(state)
        val summary = state.toSummary()
        WeightBackupStatus(
            pending = ops.isNotEmpty(),
            lastSuccess = state?.lastSuccessMillis?.let(Instant::ofEpochMilli),
            notice = when {
                conflict != null -> WeightBackupNotice.Conflict(conflict)
                summary != null -> WeightBackupNotice.Imported(summary)
                else -> null
            },
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
            val current = syncDao.observeState().first() ?: return@withLock
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
        val state = syncDao.observeState().first()
        val stored = conflictOf(state)
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
        val previous = state?.lastSyncedUid?.let(::UserId)
        val uidChanged = previous != null && previous != command.uid
        val full = command.start == WeightSyncStart.SignIn ||
            state?.cursorMillis == null ||
            uidChanged
        val remote = cloud.readOrNull(full, command.uid.value, state?.cursorMillis)
        val local = localEntries()
        return if (remote == null) {
            WeightSyncResult.Unavailable
        } else {
            applyPlan(command.uid, local, state, remote, full)
        }
    }

    private suspend fun applyPlan(
        uid: UserId,
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
                lastSyncedUid = state?.lastSyncedUid?.let(::UserId),
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
        uid: UserId,
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
        return if (cloud.writeOrFalse(uid.value, uploads)) {
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
        val state = syncDao.observeState().first()
        if (conflictOf(state) == null) {
            error("No weight conflict to resolve")
        }
        return when (choice) {
            WeightConflictChoice.KeepThisPhone -> keepPhone(state)

            is WeightConflictChoice.Cloud -> when (choice) {
                is WeightConflictChoice.Cloud.UseBackup -> useBackup(choice.uid, state)
                is WeightConflictChoice.Cloud.ReplaceBackup -> replaceBackup(choice.uid, state)
            }
        }
    }

    private suspend fun keepPhone(state: WeightSyncStateEntity?): WeightSyncResult {
        syncDao.clearOps()
        val current = state ?: emptyState()
        syncDao.upsertState(current.withoutConflict())
        return WeightSyncResult.Completed
    }

    private suspend fun useBackup(uid: UserId, previous: WeightSyncStateEntity?): WeightSyncResult {
        val remote = cloud.readOrNull(full = true, uid = uid.value, cursor = null)
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
        uid: UserId,
        previous: WeightSyncStateEntity?,
    ): WeightSyncResult {
        val remote = cloud.readOrNull(full = true, uid = uid.value, cursor = null)
        return if (remote == null) {
            WeightSyncResult.Unavailable
        } else {
            replaceCloud(uid, previous, remote)
        }
    }

    private suspend fun replaceCloud(
        uid: UserId,
        previous: WeightSyncStateEntity?,
        remote: List<RemoteWeight>,
    ): WeightSyncResult {
        val local = localEntries()
        val localDates = local.map { it.date }.toSet()
        val uploads = local.map { it.toUpload() } + remote
            .filter { !it.deleted && it.date !in localDates }
            .map { WeightUpload(it.date, weight = null, deleted = true) }
        return if (cloud.writeOrFalse(uid.value, uploads)) {
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

    private suspend fun pendingOps(): List<WeightSyncOp> =
        syncDao.observeOps().first().map { it.toOp() }

    private suspend fun storeConflict(previous: WeightSyncStateEntity?, conflict: WeightConflict) {
        val base = previous ?: emptyState()
        syncDao.upsertState(base.withConflict(conflict))
    }

    private suspend fun saveFinished(
        uid: UserId,
        previous: WeightSyncStateEntity?,
        cursorMillis: Long?,
        wrote: Boolean,
        summary: WeightImportSummary?,
        keepPreviousSummary: Boolean,
    ) {
        val now = clock.instant().toEpochMilli()
        val cursor = listOfNotNull(
            previous?.cursorMillis,
            cursorMillis,
            if (wrote || cursorMillis == null) now else null,
        ).max()
        val pendingLeft = syncDao.observeOps().first().isNotEmpty()
        val resolved = when {
            summary != null -> summary
            keepPreviousSummary -> previous.toSummary()
            else -> null
        }
        syncDao.upsertState(
            WeightSyncStateEntity(
                lastSyncedUid = uid.value,
                cursorMillis = cursor,
                lastSuccessMillis = if (pendingLeft) previous?.lastSuccessMillis else now,
                conflictKind = null,
                conflictSignedInUid = null,
                conflictPreviousUid = null,
                summaryCount = resolved?.count,
                summaryEarliest = resolved?.span?.earliest,
                summaryLatest = resolved?.span?.latest,
            ),
        )
    }

    private fun emptyState(): WeightSyncStateEntity = WeightSyncStateEntity(
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
            conflictSignedInUid = conflict.signedIn.value,
            conflictPreviousUid = conflict.previous.value,
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
}

private fun conflictOf(state: WeightSyncStateEntity?): WeightConflict? {
    val kind = state?.conflictKind ?: return null
    return when (kind) {
        WeightSyncStateEntity.ACCOUNT -> WeightConflict.DifferentAccount(
            signedIn = UserId(
                state.conflictSignedInUid
                    ?: error("Stored account conflict is missing the signed-in account"),
            ),
            previous = UserId(
                state.conflictPreviousUid
                    ?: error("Stored account conflict is missing the previous account"),
            ),
        )

        WeightSyncStateEntity.DATES -> WeightConflict.OverlappingDates

        else -> error("Unknown weight conflict '$kind'")
    }
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
        WeightImportSummary(count, WeightDateSpan(earliest, latest))
    }
}

private fun WeightSyncOpEntity.toOp(): WeightSyncOp = when (kind) {
    WeightSyncOpEntity.DELETE -> WeightSyncOp.Delete(date)

    WeightSyncOpEntity.UPSERT -> WeightSyncOp.Upsert(
        date = date,
        weight = BodyWeight(checkNotNull(weight) { "Upsert is missing its weight" }),
    )

    else -> error("Unknown weight sync op '$kind'")
}

private fun BodyWeightEntry.toEntity(): WeightEntryEntity = WeightEntryEntity(date, weight.value)

private fun BodyWeightEntry.toUpload(): WeightUpload = WeightUpload(
    date = date,
    weight = weight.value,
    deleted = false,
)

private fun List<RemoteWeight>.toLiveEntries(): List<BodyWeightEntry> = mapNotNull { remote ->
    val weight = remote.weight
    if (remote.deleted || weight == null) null else BodyWeightEntry(remote.date, weight)
}

private suspend fun WeightCloud.readOrNull(
    full: Boolean,
    uid: String,
    cursor: Long?,
): List<RemoteWeight>? = try {
    if (full || cursor == null) readAll(uid) else readUpdatedAfter(uid, cursor)
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: FirebaseException) {
    null
} catch (_: IOException) {
    null
}

private suspend fun WeightCloud.writeOrFalse(uid: String, documents: List<WeightUpload>): Boolean =
    try {
        if (documents.isNotEmpty()) write(uid, documents)
        true
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: FirebaseException) {
        false
    } catch (_: IOException) {
        false
    }
