package dev.sanastasov.bybon.account.ui

import java.time.LocalDate

data class AccountUiState(
    val environmentName: String,
    val email: String? = null,
    val message: String? = null,
    val confirmSignOut: Boolean = false,
    val signingIn: Boolean = false,
    val backupPending: Boolean = false,
    val lastBackupMillis: Long? = null,
    val conflict: Boolean = false,
    val importSummary: AccountImportSummary? = null,
) {
    val signedIn: Boolean
        get() = email != null

    val conflictChoices: List<AccountAction>
        get() = if (conflict) {
            listOf(
                AccountAction.UseTheBackup,
                AccountAction.ReplaceTheBackup,
                AccountAction.KeepThisPhone,
            )
        } else {
            emptyList()
        }
}

data class AccountImportSummary(
    val count: Int,
    val earliest: LocalDate,
    val latest: LocalDate,
)

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
