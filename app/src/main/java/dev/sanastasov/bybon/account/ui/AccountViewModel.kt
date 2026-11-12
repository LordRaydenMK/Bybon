package dev.sanastasov.bybon.account.ui

import androidx.credentials.exceptions.ClearCredentialException
import dev.sanastasov.bybon.account.domain.AccountAuth
import dev.sanastasov.bybon.account.domain.GoogleSignIn
import dev.sanastasov.bybon.account.domain.SignInFailure
import dev.sanastasov.bybon.bodyweight.domain.BodyWeightRepository
import dev.sanastasov.bybon.bodyweight.domain.WeightConflictChoice
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncCommand
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncStart
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
    private val repository: BodyWeightRepository,
    private val coroutineScope: CoroutineScope,
) {

    private val session = MutableStateFlow(SessionUi())

    val uiState = combine(
        accountAuth.user(),
        session,
        repository.backupStatus(),
    ) { user, sessionUi, backupStatus ->
        AccountUiState(
            environmentName = environmentName,
            email = user?.email,
            message = sessionUi.message,
            confirmSignOut = sessionUi.confirmSignOut,
            signingIn = sessionUi.signingIn,
            backupPending = backupStatus.pending,
            lastBackupMillis = backupStatus.lastSuccessMillis,
            notice = backupStatus.notice,
        )
    }.stateInWhileInForeground(
        coroutineScope,
        AccountUiState(environmentName),
    )

    init {
        coroutineScope.launch {
            val uid = accountAuth.user().first()?.uid ?: return@launch
            repository.sync(WeightSyncCommand.Pull(uid, WeightSyncStart.Incremental))
        }
    }

    fun onAction(action: AccountAction) {
        when (action) {
            AccountAction.OnSignInClicked -> signIn()
            AccountAction.OnSignOutClicked -> session.update { it.copy(confirmSignOut = true) }
            AccountAction.OnSignOutConfirmed -> signOut()
            AccountAction.OnSignOutDismissed -> session.update { it.copy(confirmSignOut = false) }
            AccountAction.UseTheBackup -> useBackup()
            AccountAction.ReplaceTheBackup -> replaceBackup()
            AccountAction.KeepThisPhone -> keepThisPhone()
            AccountAction.DismissImportSummary -> dismissSummary()
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
                    repository.sync(WeightSyncCommand.Pull(uid, WeightSyncStart.SignIn))
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

    private fun useBackup() {
        coroutineScope.launch {
            val uid = accountAuth.user().first()?.uid ?: return@launch
            repository.sync(WeightSyncCommand.Resolve(WeightConflictChoice.Cloud.UseBackup(uid)))
        }
    }

    private fun replaceBackup() {
        coroutineScope.launch {
            val uid = accountAuth.user().first()?.uid ?: return@launch
            repository.sync(
                WeightSyncCommand.Resolve(WeightConflictChoice.Cloud.ReplaceBackup(uid)),
            )
        }
    }

    private fun keepThisPhone() {
        coroutineScope.launch {
            repository.sync(WeightSyncCommand.Resolve(WeightConflictChoice.KeepThisPhone))
            endSession()
        }
    }

    private fun dismissSummary() {
        coroutineScope.launch { repository.dismissSummary() }
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
