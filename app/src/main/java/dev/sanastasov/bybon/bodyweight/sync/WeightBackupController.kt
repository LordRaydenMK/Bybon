package dev.sanastasov.bybon.bodyweight.sync

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.bodyweight.domain.BodyWeightRepository
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WeightBackupController(
    private val repository: BodyWeightRepository,
    private val cloud: WeightCloud,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : WeightBackup {

    private val gate = Mutex()

    override fun observe(databaseName: String): Flow<WeightBackupStatus> = combine(
        repository.observePendingOps(),
        repository.observeSyncState(databaseName),
    ) { ops, state ->
        WeightBackupStatus(
            pending = ops.isNotEmpty(),
            lastSuccessMillis = state?.lastSuccessMillis,
            conflict = state?.conflictPending == true,
            summary = state.toSummary(),
        )
    }

    override suspend fun sync(databaseName: String, uid: String, signIn: Boolean) {
        gate.withLock {
            runSync(databaseName, uid, signIn)
        }
    }

    override suspend fun keepThisPhone(databaseName: String) {
        gate.withLock {
            trySync {
                repository.clearPendingOps()
                val current = repository.syncState(databaseName).orEmpty(databaseName)
                repository.saveSyncState(
                    current.copy(
                        conflictPending = false,
                        summaryCount = null,
                        summaryEarliest = null,
                        summaryLatest = null,
                    ),
                )
            }
        }
    }

    override suspend fun useBackup(databaseName: String, uid: String) {
        gate.withLock {
            trySync {
                val remote = cloud.readAll(uid)
                val live = remote.toLiveEntries()
                repository.replaceAll(live)
                repository.clearPendingOps()
                saveFinished(
                    databaseName = databaseName,
                    uid = uid,
                    previous = repository.syncState(databaseName),
                    cursorMillis = remote.maxOfOrNull { it.updatedAtMillis },
                    wrote = false,
                    summary = live.toImportSummary(),
                    keepPreviousSummary = false,
                )
            }
        }
    }

    override suspend fun replaceBackup(databaseName: String, uid: String) {
        gate.withLock {
            trySync {
                val local = repository.entries().first()
                val remote = cloud.readAll(uid)
                val localDates = local.map { it.date }.toSet()
                val uploads = local.map { it.toUpload(deleted = false) } + remote
                    .filter { !it.deleted && it.date !in localDates }
                    .map { WeightUpload(it.date, weight = null, deleted = true) }
                if (uploads.isNotEmpty()) cloud.write(uid, uploads)
                repository.clearPendingOps()
                saveFinished(
                    databaseName = databaseName,
                    uid = uid,
                    previous = repository.syncState(databaseName),
                    cursorMillis = remote.maxOfOrNull { it.updatedAtMillis },
                    wrote = uploads.isNotEmpty(),
                    summary = null,
                    keepPreviousSummary = false,
                )
            }
        }
    }

    override suspend fun dismissSummary(databaseName: String) {
        gate.withLock {
            val current = repository.syncState(databaseName) ?: return@withLock
            repository.saveSyncState(
                current.copy(
                    summaryCount = null,
                    summaryEarliest = null,
                    summaryLatest = null,
                ),
            )
        }
    }

    private suspend fun runSync(databaseName: String, uid: String, signIn: Boolean) {
        try {
            syncBody(databaseName, uid, signIn)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // A failed write stays pending. The Account screen does not show an error.
        }
    }

    private suspend fun syncBody(databaseName: String, uid: String, signIn: Boolean) {
        val state = repository.syncState(databaseName)
        if (state?.conflictPending == true) return
        val local = repository.entries().first()
        val pending = repository.pendingOps()
        val uidChanged = state?.lastSyncedUid != null && state.lastSyncedUid != uid
        val full = signIn || state?.cursorMillis == null || uidChanged
        val remote = if (full) {
            cloud.readAll(uid)
        } else {
            cloud.readUpdatedAfter(uid, checkNotNull(state.cursorMillis))
        }
        val plan = reconcileWeights(
            WeightSyncInput(
                local = local,
                remote = remote,
                pending = pending,
                fullSnapshot = full,
                signedInUid = uid,
                lastSyncedUid = state?.lastSyncedUid,
                conflictPending = false,
            ),
        )
        when (plan) {
            WeightSyncPlan.Blocked -> Unit

            WeightSyncPlan.Ask -> repository.saveSyncState(
                state.orEmpty(databaseName).copy(
                    conflictPending = true,
                    summaryCount = null,
                    summaryEarliest = null,
                    summaryLatest = null,
                ),
            )

            is WeightSyncPlan.Apply -> apply(
                databaseName = databaseName,
                uid = uid,
                local = local,
                state = state,
                plan = plan,
            )
        }
    }

    private suspend fun apply(
        databaseName: String,
        uid: String,
        local: List<BodyWeightEntry>,
        state: WeightSyncState?,
        plan: WeightSyncPlan.Apply,
    ) {
        plan.replaceLocal?.let { repository.replaceAll(it) }
        if (plan.addLocal.isNotEmpty()) repository.addAll(plan.addLocal)
        val uploads = buildList {
            if (plan.uploadAllLocal) addAll(local.map { it.toUpload(deleted = false) })
            addAll(plan.uploads)
        }
        if (uploads.isNotEmpty()) cloud.write(uid, uploads)
        if (plan.uploadAllLocal) {
            repository.clearPendingOps()
        } else if (plan.uploads.isNotEmpty()) {
            repository.removePendingOps(plan.uploads.map { it.date })
        }
        saveFinished(
            databaseName = databaseName,
            uid = uid,
            previous = state,
            cursorMillis = plan.cursorMillis,
            wrote = uploads.isNotEmpty(),
            summary = plan.summary,
            keepPreviousSummary = plan.summary == null,
        )
    }

    private suspend fun saveFinished(
        databaseName: String,
        uid: String,
        previous: WeightSyncState?,
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
        val pendingLeft = repository.pendingOps().isNotEmpty()
        val resolved = when {
            summary != null -> summary
            keepPreviousSummary -> previous.toSummary()
            else -> null
        }
        repository.saveSyncState(
            WeightSyncState(
                databaseName = databaseName,
                lastSyncedUid = uid,
                cursorMillis = cursor,
                lastSuccessMillis = if (pendingLeft) previous?.lastSuccessMillis else now,
                conflictPending = false,
                summaryCount = resolved?.count,
                summaryEarliest = resolved?.earliest,
                summaryLatest = resolved?.latest,
            ),
        )
    }

    private suspend inline fun trySync(block: suspend () -> Unit) {
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // A failed write stays pending. The Account screen does not show an error.
        }
    }
}

private fun WeightSyncState?.orEmpty(databaseName: String): WeightSyncState =
    this ?: WeightSyncState(
        databaseName = databaseName,
        lastSyncedUid = null,
        cursorMillis = null,
        lastSuccessMillis = null,
        conflictPending = false,
        summaryCount = null,
        summaryEarliest = null,
        summaryLatest = null,
    )

private fun WeightSyncState?.toSummary(): WeightImportSummary? {
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

private fun List<RemoteWeight>.toLiveEntries(): List<BodyWeightEntry> =
    filter { !it.deleted && it.weight.isSyncableWeight() }
        .map { BodyWeightEntry(it.date, BodyWeight(it.weight)) }

private fun BodyWeightEntry.toUpload(deleted: Boolean): WeightUpload = WeightUpload(
    date = date,
    weight = if (deleted) null else weight.value,
    deleted = deleted,
)
