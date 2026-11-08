package dev.sanastasov.bybon.account.ui

data class AccountUiState(
    val environmentName: String,
    val email: String? = null,
    val message: String? = null,
    val confirmSignOut: Boolean = false,
    val signingIn: Boolean = false,
    val backupPending: Boolean = false,
    val lastBackupMillis: Long? = null,
) {
    val signedIn: Boolean
        get() = email != null
}

sealed interface AccountAction {
    data object OnSignInClicked : AccountAction
    data object OnSignOutClicked : AccountAction
    data object OnSignOutConfirmed : AccountAction
    data object OnSignOutDismissed : AccountAction
}
