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
import dev.sanastasov.bybon.account.AccountModule
import dev.sanastasov.bybon.account.data.CredentialManagerGoogleSignIn
import dev.sanastasov.bybon.ui.components.BybonTopAppBar
import dev.sanastasov.bybon.ui.theme.BybonTheme

@Composable
fun AccountModule.AccountScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel = retain {
        AccountViewModel(
            accountAuth,
            CredentialManagerGoogleSignIn(context),
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
}

@Preview
@Composable
private fun AccountScreenSignedOutPreview() {
    BybonTheme {
        AccountScreenContent(
            AccountUiState(),
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
            AccountUiState(email = "stolea@gmail.com"),
            {},
            {},
        )
    }
}
