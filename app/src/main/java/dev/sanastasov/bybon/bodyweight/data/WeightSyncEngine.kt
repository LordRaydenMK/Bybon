package dev.sanastasov.bybon.bodyweight.data

import com.google.firebase.FirebaseException
import dev.sanastasov.bybon.account.domain.UserId
import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.bodyweight.domain.WeightBackupStatus
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
import java.io.IOException
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
        WeightBackupStatus(
            pending = ops.isNotEmpty(),
            lastSuccess = state?.lastSuccessMillis?.let(Instant::ofEpochMilli),
            summary = state.toSummary(),
        )
    }

    suspend fun sync(command: WeightSyncCommand): WeightSyncResult = gate.withLock {
        pull(command)
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

    private suspend fun pull(command: WeightSyncCommand): WeightSyncResult {
        val state = syncDao.observeState().first()
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
            ),
        )
        return when (plan) {
            is WeightSyncPlan.Conflict -> WeightSyncResult.Conflict(plan.conflict)
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

    private suspend fun replaceWeights(entries: List<BodyWeightEntry>) {
        weights.deleteAllWeights()
        if (entries.isNotEmpty()) weights.insertWeights(entries.map { it.toEntity() })
    }

    private suspend fun localEntries(): List<BodyWeightEntry> =
        weights.weightEntries().first().map { BodyWeightEntry(it.date, BodyWeight(it.weight)) }

    private suspend fun pendingOps(): List<WeightSyncOp> =
        syncDao.observeOps().first().map { it.toOp() }

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
                summaryCount = resolved?.count,
                summaryEarliest = resolved?.span?.earliest,
                summaryLatest = resolved?.span?.latest,
            ),
        )
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
