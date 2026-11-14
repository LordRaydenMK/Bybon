package dev.sanastasov.bybon.account.ui

import dev.sanastasov.bybon.bodyweight.domain.WeightConflict
import dev.sanastasov.bybon.bodyweight.domain.WeightImportSummary
import java.time.Instant

data class AccountUiState(
    val environmentName: String,
    val email: String? = null,
    val message: String? = null,
    val confirmSignOut: Boolean = false,
    val signingIn: Boolean = false,
    val backupPending: Boolean = false,
    val lastBackup: Instant? = null,
    val summary: WeightImportSummary? = null,
    val conflict: WeightConflict? = null,
) {
    val signedIn: Boolean
        get() = email != null
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
    data object DismissImportSummary : AccountAction
}
