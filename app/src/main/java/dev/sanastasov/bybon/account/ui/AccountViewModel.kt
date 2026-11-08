package dev.sanastasov.bybon.account.ui

import androidx.credentials.exceptions.ClearCredentialException
import dev.sanastasov.bybon.account.domain.AccountAuth
import dev.sanastasov.bybon.account.domain.GoogleSignIn
import dev.sanastasov.bybon.account.domain.SignInFailure
import dev.sanastasov.bybon.bodyweight.sync.WeightBackup
import dev.sanastasov.bybon.bodyweight.sync.WeightSyncScheduler
import dev.sanastasov.bybon.ui.stateInWhileInForeground
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountViewModel(
    private val accountAuth: AccountAuth,
    private val googleSignIn: GoogleSignIn,
    private val environmentName: String,
    private val backup: WeightBackup,
    private val weightSyncScheduler: WeightSyncScheduler,
    private val coroutineScope: CoroutineScope,
) {

    private val session = MutableStateFlow(SessionUi())

    val uiState = combine(
        accountAuth.user(),
        session,
        backup.observe(environmentName),
    ) { user, sessionUi, backupStatus ->
        AccountUiState(
            environmentName = environmentName,
            email = user?.email,
            message = sessionUi.message,
            confirmSignOut = sessionUi.confirmSignOut,
            signingIn = sessionUi.signingIn,
            backupPending = backupStatus.pending,
            lastBackupMillis = backupStatus.lastSuccessMillis,
        )
    }.stateInWhileInForeground(
        coroutineScope,
        AccountUiState(environmentName),
    )

    init {
        coroutineScope.launch {
            val uid = accountAuth.user().first()?.uid ?: return@launch
            backup.sync(environmentName, uid, signIn = false)
        }
    }

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
                val uid = accountAuth.user().first()?.uid
                if (uid != null) {
                    weightSyncScheduler.enqueueImmediate(signIn = true)
                    backup.sync(environmentName, uid, signIn = true)
                }
                session.update { it.copy(signingIn = false, message = null) }
            } catch (_: SignInFailure.Canceled) {
                session.update { it.copy(signingIn = false, message = "Sign-in canceled") }
            } catch (_: SignInFailure.Failed) {
                session.update { it.copy(signingIn = false, message = "Sign-in failed") }
            }
        }
    }

    private fun signOut() {
        coroutineScope.launch { endSession() }
    }

    private suspend fun endSession() {
        accountAuth.signOut()
        try {
            googleSignIn.clearCredentialState()
        } catch (_: ClearCredentialException) {
            // Firebase session already ended.
        }
        session.value = SessionUi()
    }

    private data class SessionUi(
        val message: String? = null,
        val confirmSignOut: Boolean = false,
        val signingIn: Boolean = false,
    )
}
