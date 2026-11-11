package dev.sanastasov.bybon.bodyweight.domain

import dev.sanastasov.bybon.bodyweight.BodyWeight
import java.time.LocalDate

/** SignIn reads the whole cloud. Incremental uses the cursor. */
enum class WeightSyncStart { SignIn, Incremental }

/** The answer to a stored conflict. Cloud choices name the account they write. */
sealed interface WeightConflictChoice {
    data object KeepThisPhone : WeightConflictChoice

    data class UseBackup(
        val uid: String,
    ) : WeightConflictChoice

    data class ReplaceBackup(
        val uid: String,
    ) : WeightConflictChoice
}

sealed interface WeightSyncCommand {
    data class Pull(
        val uid: String,
        val start: WeightSyncStart,
    ) : WeightSyncCommand

    data class Resolve(
        val choice: WeightConflictChoice,
    ) : WeightSyncCommand
}

/** A live backup value, or a tombstone for that date. */
sealed interface CloudWeight {
    data class Value(
        val weight: BodyWeight,
    ) : CloudWeight

    data object Deleted : CloudWeight
}

data class WeightDateClash(
    val date: LocalDate,
    val onThisPhone: BodyWeight,
    val inBackup: CloudWeight,
)

/** Why a sync stopped before changing the phone or the backup. */
sealed interface WeightConflict {
    data class DifferentAccount(
        val signedInUid: String,
        val previousUid: String,
    ) : WeightConflict

    data class OverlappingDates(
        val dates: List<WeightDateClash>,
    ) : WeightConflict
}

/**
 * Completed: this call finished.
 * NeedsChoice: nothing was applied. [conflict] is stored.
 * Unavailable: the remote read or write did not finish. Pending ops stay.
 */
sealed interface WeightSyncResult {
    data object Completed : WeightSyncResult

    data class NeedsChoice(
        val conflict: WeightConflict,
    ) : WeightSyncResult

    data object Unavailable : WeightSyncResult
}

data class WeightImportSummary(
    val count: Int,
    val earliest: LocalDate,
    val latest: LocalDate,
)

data class WeightBackupStatus(
    val pending: Boolean = false,
    val lastSuccessMillis: Long? = null,
    val conflict: WeightConflict? = null,
    val summary: WeightImportSummary? = null,
)

data class WeightSyncOp(
    val date: LocalDate,
    val kind: WeightSyncOpKind,
    val weight: Int?,
)

enum class WeightSyncOpKind { Upsert, Delete }
