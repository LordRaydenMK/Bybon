package dev.sanastasov.bybon.account.ui

import dev.sanastasov.bybon.account.FakeAccountAuth
import dev.sanastasov.bybon.account.FakeGoogleSignIn
import dev.sanastasov.bybon.account.domain.AccountUser
import dev.sanastasov.bybon.account.domain.SignInFailure
import dev.sanastasov.bybon.account.domain.UserId
import dev.sanastasov.bybon.bodyweight.FakeBodyWeightRepository
import dev.sanastasov.bybon.bodyweight.domain.WeightBackupStatus
import dev.sanastasov.bybon.bodyweight.domain.WeightConflict
import dev.sanastasov.bybon.bodyweight.domain.WeightDateSpan
import dev.sanastasov.bybon.bodyweight.domain.WeightImportSummary
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncCommand
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncResult
import dev.sanastasov.bybon.bodyweight.domain.WeightSyncStart
import dev.sanastasov.bybon.test.BackgroundFailures
import java.time.Instant
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
        val viewModel = viewModel(FakeAccountAuth(AccountUser(UserId("uid"), "stolea@gmail.com")))

        viewModel.uiState.first { it.email == "stolea@gmail.com" }
        viewModel.onAction(AccountAction.OnSignOutClicked)
        assert(viewModel.uiState.first { it.confirmSignOut }.email == "stolea@gmail.com")

        viewModel.onAction(AccountAction.OnSignOutDismissed)
        val dismissed = viewModel.uiState.first { !it.confirmSignOut }
        assert(dismissed.email == "stolea@gmail.com")
    }

    @Test
    fun `confirming sign-out returns to signed out`() = runTest {
        val auth = FakeAccountAuth(AccountUser(UserId("uid"), "stolea@gmail.com"))
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
        val weights = FakeBodyWeightRepository()
        weights.backup.value = WeightBackupStatus(
            pending = true,
            lastSuccess = Instant.ofEpochMilli(50),
        )
        val viewModel = viewModel(weights = weights)

        val state = viewModel.uiState.first { it.backupPending }

        assert(state.backupPending)
        assert(state.lastBackup == Instant.ofEpochMilli(50))
    }

    @Test
    fun `finished sync shows the last success time without pending`() = runTest {
        val weights = FakeBodyWeightRepository()
        weights.backup.value = WeightBackupStatus(
            pending = false,
            lastSuccess = Instant.ofEpochMilli(99),
        )
        val viewModel = viewModel(weights = weights)

        val state = viewModel.uiState.first { it.lastBackup == Instant.ofEpochMilli(99) }

        assert(!state.backupPending)
        assert(state.lastBackup == Instant.ofEpochMilli(99))
    }

    @Test
    fun `a conflict is shown without a choice`() = runTest {
        val weights = FakeBodyWeightRepository()
        val conflict = WeightConflict.OverlappingDates
        weights.nextSyncResult = WeightSyncResult.Conflict(conflict)
        val viewModel = viewModel(
            auth = FakeAccountAuth(AccountUser(UserId("uid"), "stolea@gmail.com")),
            weights = weights,
        )

        val state = viewModel.uiState.first { it.conflict != null }

        assert(state.conflict == conflict)
        assert(conflict.detail() == "The backup and this phone have different weights.")
    }

    @Test
    fun `opening account while signed in syncs without a full sign-in read`() = runTest {
        val weights = FakeBodyWeightRepository()
        val viewModel = viewModel(
            auth = FakeAccountAuth(AccountUser(UserId("uid"), "stolea@gmail.com")),
            weights = weights,
        )

        viewModel.uiState.first { it.email == "stolea@gmail.com" }
        yield()

        assert(
            weights.syncs.single() == WeightSyncCommand(
                UserId("uid"),
                WeightSyncStart.Incremental,
            ),
        )
    }

    @Test
    fun `sign-in starts a full sync for that uid`() = runTest {
        val weights = FakeBodyWeightRepository()
        val viewModel = viewModel(weights = weights)

        viewModel.uiState.first { !it.signingIn }
        viewModel.onAction(AccountAction.OnSignInClicked)
        viewModel.uiState.first { it.email == "token@example.com" }
        yield()

        assert(
            weights.syncs.single() == WeightSyncCommand(
                UserId("token"),
                WeightSyncStart.SignIn,
            ),
        )
    }

    @Test
    fun `import summary is offered until it is dismissed`() = runTest {
        val weights = FakeBodyWeightRepository()
        val summary = WeightImportSummary(
            count = 2,
            span = WeightDateSpan(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1)),
        )
        weights.backup.value = WeightBackupStatus(summary = summary)
        val viewModel = viewModel(weights = weights)

        val showing = viewModel.uiState.first { it.summary != null }
        assert(showing.summary?.count == 2)

        viewModel.onAction(AccountAction.DismissImportSummary)
        val dismissed = viewModel.uiState.first { it.summary == null }
        assert(dismissed.summary == null)
    }

    private fun kotlinx.coroutines.test.TestScope.viewModel(
        auth: FakeAccountAuth = FakeAccountAuth(),
        googleSignIn: FakeGoogleSignIn = FakeGoogleSignIn(),
        environmentName: String = "prd",
        weights: FakeBodyWeightRepository = FakeBodyWeightRepository(),
        coroutineScope: CoroutineScope = backgroundScope,
    ) = AccountViewModel(auth, googleSignIn, environmentName, weights, coroutineScope)
}
