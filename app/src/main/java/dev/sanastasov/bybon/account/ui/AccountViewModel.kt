package dev.sanastasov.bybon.account.ui

import dev.sanastasov.bybon.account.domain.AccountAuth
import dev.sanastasov.bybon.account.domain.GoogleSignIn
import dev.sanastasov.bybon.account.domain.SignInFailure
import dev.sanastasov.bybon.ui.stateInWhileInForeground
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountViewModel(
    private val accountAuth: AccountAuth,
    private val googleSignIn: GoogleSignIn,
    environmentName: String,
    private val coroutineScope: CoroutineScope,
) {

    private val session = MutableStateFlow(SessionUi())

    val uiState = combine(accountAuth.user(), session) { user, sessionUi ->
        AccountUiState(
            environmentName = environmentName,
            email = user?.email,
            message = sessionUi.message,
            confirmSignOut = sessionUi.confirmSignOut,
            signingIn = sessionUi.signingIn,
        )
    }.stateInWhileInForeground(
        coroutineScope,
        AccountUiState(environmentName),
    )

    fun onAction(action: AccountAction) {
        when (action) {
            AccountAction.OnSignInClicked -> signIn()
            AccountAction.OnSignOutClicked -> session.update { it.copy(confirmSignOut = true) }
            AccountAction.OnSignOutConfirmed -> signOut()
            AccountAction.OnSignOutDismissed -> session.update { it.copy(confirmSignOut = false) }
        }
    }

    private fun signIn() {
        if (session.value.signingIn) return
        session.update { it.copy(signingIn = true, message = null) }
        coroutineScope.launch {
            try {
                accountAuth.signInWithIdToken(googleSignIn.idToken())
                session.update { it.copy(signingIn = false, message = null) }
            } catch (e: CancellationException) {
                session.update { it.copy(signingIn = false) }
                throw e
            } catch (_: SignInFailure.Canceled) {
                session.update { it.copy(signingIn = false, message = "Sign-in canceled") }
            } catch (_: Exception) {
                session.update { it.copy(signingIn = false, message = "Sign-in failed") }
            }
        }
    }

    private fun signOut() {
        coroutineScope.launch {
            accountAuth.signOut()
            googleSignIn.clearCredentialState()
            session.value = SessionUi()
        }
    }

    private data class SessionUi(
        val message: String? = null,
        val confirmSignOut: Boolean = false,
        val signingIn: Boolean = false,
    )
}
