package dev.sanastasov.bybon.account.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.marcellogalhardo.retained.compose.retain
import dev.sanastasov.bybon.account.data.CredentialManagerGoogleSignIn
import dev.sanastasov.bybon.bodyweight.domain.WeightImportSummary
import dev.sanastasov.bybon.domain.toDisplayDate
import dev.sanastasov.bybon.main.MainModule
import dev.sanastasov.bybon.ui.components.BybonTopAppBar
import dev.sanastasov.bybon.ui.theme.BybonTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MainModule.AccountScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel = retain {
        AccountViewModel(
            accountAuth,
            CredentialManagerGoogleSignIn(context),
            firebaseEnvName,
            bodyWeightRepository,
            it.coroutineScope,
        )
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AccountScreenContent(uiState, viewModel::onAction, onNavigateBack)
}

@Composable
fun AccountScreenContent(
    uiState: AccountUiState,
    onAction: (AccountAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier.fillMaxSize(),
        topBar = { BybonTopAppBar("Account", onNavigateBack) },
    ) { contentPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(uiState.environmentName, style = MaterialTheme.typography.titleMedium)
            BackupStatus(uiState)
            uiState.conflict?.let { conflict ->
                Text(conflict.detail(), style = MaterialTheme.typography.bodyMedium)
            }
            if (uiState.signedIn) {
                Text(uiState.email.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                OutlinedButton(
                    { onAction(AccountAction.OnSignOutClicked) },
                    Modifier.fillMaxWidth(),
                ) {
                    Text("Sign out")
                }
            } else {
                Button(
                    { onAction(AccountAction.OnSignInClicked) },
                    Modifier.fillMaxWidth(),
                    enabled = !uiState.signingIn,
                ) {
                    Text("Sign in with Google")
                }
                uiState.message?.let { message ->
                    Text(message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    if (uiState.confirmSignOut) {
        AlertDialog(
            onDismissRequest = { onAction(AccountAction.OnSignOutDismissed) },
            title = { Text("Sign out?") },
            confirmButton = {
                TextButton({ onAction(AccountAction.OnSignOutConfirmed) }) { Text("Sign out") }
            },
            dismissButton = {
                TextButton({ onAction(AccountAction.OnSignOutDismissed) }) { Text("Cancel") }
            },
        )
    }
    uiState.summary?.let { summary -> WeightImportDialog(summary, onAction) }
}

@Composable
private fun BackupStatus(uiState: AccountUiState) {
    if (uiState.backupPending) {
        Text("Backup pending", style = MaterialTheme.typography.bodyMedium)
    }
    uiState.lastBackup?.let { instant ->
        Text(
            "Last backup ${formatLastBackup(instant)}",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun WeightImportDialog(summary: WeightImportSummary, onAction: (AccountAction) -> Unit) {
    val noun = if (summary.count == 1) "weight" else "weights"
    AlertDialog(
        onDismissRequest = { onAction(AccountAction.DismissImportSummary) },
        title = { Text("Weights imported") },
        text = {
            Text(
                "Imported ${summary.count} $noun, from " +
                    "${summary.span.earliest.toDisplayDate()} to ${summary.span.latest.toDisplayDate()}.",
            )
        },
        confirmButton = {
            TextButton({ onAction(AccountAction.DismissImportSummary) }) { Text("OK") }
        },
    )
}

private fun formatLastBackup(instant: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.US)
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

@Preview
@Composable
private fun AccountScreenSignedOutPreview() {
    BybonTheme {
        AccountScreenContent(
            AccountUiState("prd"),
            {},
            {},
        )
    }
}

@Preview
@Composable
private fun AccountScreenSignedInPreview() {
    BybonTheme {
        AccountScreenContent(
            AccountUiState("prd", email = "stolea@gmail.com"),
            {},
            {},
        )
    }
}
