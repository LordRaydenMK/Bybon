package dev.sanastasov.bybon.account.ui

import dev.sanastasov.bybon.account.FakeAccountAuth
import dev.sanastasov.bybon.account.FakeGoogleSignIn
import dev.sanastasov.bybon.account.domain.AccountUser
import dev.sanastasov.bybon.account.domain.SignInFailure
import dev.sanastasov.bybon.bodyweight.sync.FakeWeightBackup
import dev.sanastasov.bybon.bodyweight.sync.WeightBackupStatus
import dev.sanastasov.bybon.bodyweight.sync.WeightImportSummary
import dev.sanastasov.bybon.test.BackgroundFailures
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Test

class AccountViewModelTest {

    @Test
    fun `signed out shows the environment name`() = runTest {
        val viewModel = viewModel()

        val state = viewModel.uiState.first { !it.signingIn }

        assert(state.environmentName == "prd")
        assert(state.email == null)
    }

    @Test
    fun `successful sign-in shows the account email`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.first { !it.signingIn }
        viewModel.onAction(AccountAction.OnSignInClicked)

        val state = viewModel.uiState.first { it.email != null }
        assert(state.email == "token@example.com")
        assert(state.message == null)
    }

    @Test
    fun `canceling sign-in stays signed out and shows a short message`() = runTest {
        val viewModel = viewModel(googleSignIn = FakeGoogleSignIn.canceled())

        viewModel.uiState.first { !it.signingIn }
        viewModel.onAction(AccountAction.OnSignInClicked)

        val state = viewModel.uiState.first { it.message != null }
        assert(state.email == null)
        assert(state.message == "Sign-in canceled")
    }

    @Test
    fun `failed sign-in stays signed out and shows a short message`() = runTest {
        val viewModel = viewModel(googleSignIn = FakeGoogleSignIn.failed())

        viewModel.uiState.first { !it.signingIn }
        viewModel.onAction(AccountAction.OnSignInClicked)

        val state = viewModel.uiState.first { it.message != null }
        assert(state.email == null)
        assert(state.message == "Sign-in failed")
    }

    @Test
    fun `failed firebase sign-in stays signed out and shows a short message`() = runTest {
        val cause = IllegalStateException("auth rejected")
        val viewModel = viewModel(
            FakeAccountAuth(signInError = SignInFailure.Failed(cause)),
        )

        viewModel.uiState.first { !it.signingIn }
        viewModel.onAction(AccountAction.OnSignInClicked)

        val state = viewModel.uiState.first { it.message != null }
        assert(state.email == null)
        assert(state.message == "Sign-in failed")
    }

    @Test
    fun `misconfigured google sign-in crashes instead of showing failed`() = runTest {
        val failures = BackgroundFailures(this)
        val viewModel = viewModel(
            googleSignIn = FakeGoogleSignIn(
                Result.failure(IllegalStateException("Missing default_web_client_id")),
            ),
            coroutineScope = failures.scope,
        )

        viewModel.uiState.first { !it.signingIn }
        failures.expectFailure("Missing default_web_client_id") {
            viewModel.onAction(AccountAction.OnSignInClicked)
        }
    }

    @Test
    fun `misconfigured firebase sign-in crashes instead of showing failed`() = runTest {
        val failures = BackgroundFailures(this)
        val viewModel = viewModel(
            FakeAccountAuth(signInError = IllegalStateException("FirebaseApp not initialized")),
            coroutineScope = failures.scope,
        )

        viewModel.uiState.first { !it.signingIn }
        failures.expectFailure("FirebaseApp not initialized") {
            viewModel.onAction(AccountAction.OnSignInClicked)
        }
    }

    @Test
    fun `sign-out asks first and dismissing keeps the session`() = runTest {
        val viewModel = viewModel(FakeAccountAuth(AccountUser("uid", "stolea@gmail.com")))

        viewModel.uiState.first { it.email == "stolea@gmail.com" }
        viewModel.onAction(AccountAction.OnSignOutClicked)
        assert(viewModel.uiState.first { it.confirmSignOut }.email == "stolea@gmail.com")

        viewModel.onAction(AccountAction.OnSignOutDismissed)
        val dismissed = viewModel.uiState.first { !it.confirmSignOut }
        assert(dismissed.email == "stolea@gmail.com")
    }

    @Test
    fun `confirming sign-out returns to signed out`() = runTest {
        val auth = FakeAccountAuth(AccountUser("uid", "stolea@gmail.com"))
        val googleSignIn = FakeGoogleSignIn()
        val viewModel = viewModel(auth, googleSignIn)

        viewModel.uiState.first { it.email == "stolea@gmail.com" }
        viewModel.onAction(AccountAction.OnSignOutClicked)
        viewModel.uiState.first { it.confirmSignOut }
        viewModel.onAction(AccountAction.OnSignOutConfirmed)

        val signedOut = viewModel.uiState.first { it.email == null && !it.confirmSignOut }
        assert(signedOut.email == null)
        assert(auth.signedOut)
        assert(googleSignIn.cleared)
    }

    @Test
    fun `pending backup shows pending and the last success time`() = runTest {
        val backup = FakeWeightBackup()
        backup.status.value = WeightBackupStatus(pending = true, lastSuccessMillis = 50)
        val viewModel = viewModel(backup = backup)

        val state = viewModel.uiState.first { it.backupPending }

        assert(state.backupPending)
        assert(state.lastBackupMillis == 50L)
    }

    @Test
    fun `finished sync shows the last success time without pending`() = runTest {
        val backup = FakeWeightBackup()
        backup.status.value = WeightBackupStatus(pending = false, lastSuccessMillis = 99)
        val viewModel = viewModel(backup = backup)

        val state = viewModel.uiState.first { it.lastBackupMillis == 99L }

        assert(!state.backupPending)
        assert(state.lastBackupMillis == 99L)
    }

    @Test
    fun `conflict offers use, replace, and keep this phone`() = runTest {
        val backup = FakeWeightBackup()
        backup.status.value = WeightBackupStatus(conflict = true)
        val viewModel = viewModel(
            auth = FakeAccountAuth(AccountUser("uid", "stolea@gmail.com")),
            backup = backup,
        )

        val state = viewModel.uiState.first { it.conflict }

        assert(
            state.conflictChoices == listOf(
                AccountAction.UseTheBackup,
                AccountAction.ReplaceTheBackup,
                AccountAction.KeepThisPhone,
            ),
        )
    }

    @Test
    fun `opening account while signed in syncs without a full sign-in read`() = runTest {
        val backup = FakeWeightBackup()
        val viewModel = viewModel(
            auth = FakeAccountAuth(AccountUser("uid", "stolea@gmail.com")),
            backup = backup,
        )

        viewModel.uiState.first { it.email == "stolea@gmail.com" }
        yield()

        assert(backup.syncs.single() == ("uid" to false))
    }

    @Test
    fun `sign-in starts a full sync for that uid`() = runTest {
        val backup = FakeWeightBackup()
        val viewModel = viewModel(backup = backup)

        viewModel.uiState.first { !it.signingIn }
        viewModel.onAction(AccountAction.OnSignInClicked)
        viewModel.uiState.first { it.email == "token@example.com" }
        yield()

        assert(backup.syncs.any { it == ("token" to true) })
    }

    @Test
    fun `keep this phone signs out`() = runTest {
        val auth = FakeAccountAuth(AccountUser("uid", "stolea@gmail.com"))
        val backup = FakeWeightBackup()
        val viewModel = viewModel(auth, backup = backup)

        viewModel.uiState.first { it.email == "stolea@gmail.com" }
        viewModel.onAction(AccountAction.KeepThisPhone)

        val signedOut = viewModel.uiState.first { it.email == null }
        assert(signedOut.email == null)
        assert(auth.signedOut)
        assert(backup.kept)
    }

    @Test
    fun `import summary is offered until it is dismissed`() = runTest {
        val backup = FakeWeightBackup()
        val summary = WeightImportSummary(
            count = 2,
            earliest = LocalDate.of(2026, 1, 1),
            latest = LocalDate.of(2026, 2, 1),
        )
        backup.status.value = WeightBackupStatus(summary = summary)
        val viewModel = viewModel(backup = backup)

        val showing = viewModel.uiState.first { it.importSummary != null }
        assert(showing.importSummary?.count == 2)

        viewModel.onAction(AccountAction.DismissImportSummary)
        val dismissed = viewModel.uiState.first { it.importSummary == null }
        assert(dismissed.importSummary == null)
    }

    private fun kotlinx.coroutines.test.TestScope.viewModel(
        auth: FakeAccountAuth = FakeAccountAuth(),
        googleSignIn: FakeGoogleSignIn = FakeGoogleSignIn(),
        environmentName: String = "prd",
        backup: FakeWeightBackup = FakeWeightBackup(),
        coroutineScope: CoroutineScope = backgroundScope,
    ) = AccountViewModel(auth, googleSignIn, environmentName, backup, coroutineScope)
}
