package dev.sanastasov.bybon.bodyweight.domain

import dev.sanastasov.bybon.account.domain.UserId
import dev.sanastasov.bybon.bodyweight.BodyWeight
import java.time.Instant
import java.time.LocalDate

/** SignIn reads the whole cloud. Incremental uses the cursor. */
enum class WeightSyncStart { SignIn, Incremental }

/**
 * The answer to a stored conflict.
 * [Cloud] choices write the named account. Keep this phone writes nothing.
 */
sealed class WeightConflictChoice {
    data object KeepThisPhone : WeightConflictChoice()

    sealed class Cloud(
        open val uid: UserId,
    ) : WeightConflictChoice() {
        data class UseBackup(
            override val uid: UserId,
        ) : Cloud(uid)

        data class ReplaceBackup(
            override val uid: UserId,
        ) : Cloud(uid)
    }
}

sealed interface WeightSyncCommand {
    data class Pull(
        val uid: UserId,
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
sealed class WeightConflict {
    data class DifferentAccount(
        val signedIn: UserId,
        val previous: UserId,
    ) : WeightConflict() {
        init {
            require(signedIn != previous) { "Account conflict needs two accounts" }
        }
    }

    data class OverlappingDates(
        val dates: List<WeightDateClash>,
    ) : WeightConflict() {
        init {
            require(dates.isNotEmpty()) { "Date conflict needs a date" }
        }
    }
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

/** The dates an import covers. Earliest is never after latest. */
data class WeightDateSpan(
    val earliest: LocalDate,
    val latest: LocalDate,
) {
    init {
        require(!earliest.isAfter(latest)) { "Earliest date is after the latest" }
    }
}

data class WeightImportSummary(
    val count: Int,
    val span: WeightDateSpan,
) {
    init {
        require(count > 0) { "Import summary needs at least one weight" }
    }
}

/** One thing for the screen to show. A conflict and an import summary do not coexist. */
sealed class WeightBackupNotice {
    data class Conflict(
        val conflict: WeightConflict,
    ) : WeightBackupNotice()

    data class Imported(
        val summary: WeightImportSummary,
    ) : WeightBackupNotice()
}

data class WeightBackupStatus(
    val pending: Boolean = false,
    val lastSuccess: Instant? = null,
    val notice: WeightBackupNotice? = null,
)

/** A date waiting to be written. An upsert carries the weight. A delete does not. */
sealed class WeightSyncOp {
    abstract val date: LocalDate

    data class Upsert(
        override val date: LocalDate,
        val weight: BodyWeight,
    ) : WeightSyncOp()

    data class Delete(
        override val date: LocalDate,
    ) : WeightSyncOp()
}
