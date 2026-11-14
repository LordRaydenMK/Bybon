package dev.sanastasov.bybon.account.ui

data class AccountUiState(
    val email: String? = null,
    val message: String? = null,
    val confirmSignOut: Boolean = false,
    val signingIn: Boolean = false,
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
