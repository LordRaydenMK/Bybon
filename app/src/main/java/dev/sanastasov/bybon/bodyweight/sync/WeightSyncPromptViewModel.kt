package dev.sanastasov.bybon.bodyweight.sync

import androidx.credentials.exceptions.ClearCredentialException
import dev.sanastasov.bybon.account.domain.AccountAuth
import dev.sanastasov.bybon.account.domain.GoogleSignIn
import dev.sanastasov.bybon.ui.stateInWhileInForeground
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class WeightSyncPromptViewModel(
    private val backup: WeightBackup,
    private val accountAuth: AccountAuth,
    private val googleSignIn: GoogleSignIn,
    private val environmentName: String,
    private val coroutineScope: CoroutineScope,
) {

    val prompt = backup.observe(environmentName).map { status ->
        when {
            status.conflict -> WeightSyncPrompt.Conflict
            status.summary != null -> WeightSyncPrompt.Summary(status.summary)
            else -> WeightSyncPrompt.None
        }
    }.stateInWhileInForeground(coroutineScope, WeightSyncPrompt.None)

    fun onAction(action: WeightSyncPromptAction) {
        when (action) {
            WeightSyncPromptAction.UseTheBackup -> useBackup()
            WeightSyncPromptAction.ReplaceTheBackup -> replaceBackup()
            WeightSyncPromptAction.KeepThisPhone -> keepThisPhone()
            WeightSyncPromptAction.DismissSummary -> dismissSummary()
        }
    }

    private fun useBackup() {
        coroutineScope.launch {
            val uid = accountAuth.user().first()?.uid ?: return@launch
            backup.useBackup(environmentName, uid)
        }
    }

    private fun replaceBackup() {
        coroutineScope.launch {
            val uid = accountAuth.user().first()?.uid ?: return@launch
            backup.replaceBackup(environmentName, uid)
        }
    }

    private fun keepThisPhone() {
        coroutineScope.launch {
            backup.keepThisPhone(environmentName)
            accountAuth.signOut()
            try {
                googleSignIn.clearCredentialState()
            } catch (_: ClearCredentialException) {
                // Firebase session already ended.
            }
        }
    }

    private fun dismissSummary() {
        coroutineScope.launch { backup.dismissSummary(environmentName) }
    }
}

sealed interface WeightSyncPrompt {
    data object None : WeightSyncPrompt

    data object Conflict : WeightSyncPrompt

    data class Summary(
        val summary: WeightImportSummary,
    ) : WeightSyncPrompt

    val conflictChoices: List<WeightSyncPromptAction>
        get() = if (this is Conflict) {
            listOf(
                WeightSyncPromptAction.UseTheBackup,
                WeightSyncPromptAction.ReplaceTheBackup,
                WeightSyncPromptAction.KeepThisPhone,
            )
        } else {
            emptyList()
        }
}

sealed interface WeightSyncPromptAction {
    data object UseTheBackup : WeightSyncPromptAction
    data object ReplaceTheBackup : WeightSyncPromptAction
    data object KeepThisPhone : WeightSyncPromptAction
    data object DismissSummary : WeightSyncPromptAction
}
