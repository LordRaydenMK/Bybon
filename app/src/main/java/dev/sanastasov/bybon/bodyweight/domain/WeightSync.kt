package dev.sanastasov.bybon.bodyweight.domain

import dev.sanastasov.bybon.account.domain.UserId
import dev.sanastasov.bybon.bodyweight.BodyWeight
import java.time.Instant
import java.time.LocalDate

/** SignIn reads the whole cloud. Incremental uses the cursor. */
enum class WeightSyncStart { SignIn, Incremental }

data class WeightSyncCommand(
    val uid: UserId,
    val start: WeightSyncStart,
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

    data object OverlappingDates : WeightConflict()
}

/**
 * Completed: this call finished.
 * Conflict: nothing was applied. The phone and the backup stay as they are.
 * Unavailable: the remote read or write did not finish. Pending ops stay.
 */
sealed interface WeightSyncResult {
    data object Completed : WeightSyncResult

    data class Conflict(
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

data class WeightBackupStatus(
    val pending: Boolean = false,
    val lastSuccess: Instant? = null,
    val summary: WeightImportSummary? = null,
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
