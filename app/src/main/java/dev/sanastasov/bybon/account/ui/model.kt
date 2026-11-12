package dev.sanastasov.bybon.account.ui

import dev.sanastasov.bybon.bodyweight.domain.WeightBackupNotice
import dev.sanastasov.bybon.bodyweight.domain.WeightConflict
import java.time.Instant

data class AccountUiState(
    val environmentName: String,
    val email: String? = null,
    val message: String? = null,
    val confirmSignOut: Boolean = false,
    val signingIn: Boolean = false,
    val backupPending: Boolean = false,
    val lastBackup: Instant? = null,
    val notice: WeightBackupNotice? = null,
) {
    val signedIn: Boolean
        get() = email != null

    val conflictChoices: List<AccountAction>
        get() = if (notice is WeightBackupNotice.Conflict) {
            listOf(
                AccountAction.UseTheBackup,
                AccountAction.ReplaceTheBackup,
                AccountAction.KeepThisPhone,
            )
        } else {
            emptyList()
        }
}

fun WeightConflict.detail(): String = when (this) {
    is WeightConflict.DifferentAccount -> "This phone has weights from another account."
    WeightConflict.OverlappingDates -> "The backup and this phone have different weights."
}

sealed interface AccountAction {
    data object OnSignInClicked : AccountAction
    data object OnSignOutClicked : AccountAction
    data object OnSignOutConfirmed : AccountAction
    data object OnSignOutDismissed : AccountAction
    data object UseTheBackup : AccountAction
    data object ReplaceTheBackup : AccountAction
    data object KeepThisPhone : AccountAction
    data object DismissImportSummary : AccountAction
}
