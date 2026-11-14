package dev.sanastasov.bybon.account.ui

import dev.sanastasov.bybon.account.FakeAccountAuth
import dev.sanastasov.bybon.account.FakeGoogleSignIn
import dev.sanastasov.bybon.account.domain.AccountUser
import dev.sanastasov.bybon.account.domain.SignInFailure
import dev.sanastasov.bybon.test.BackgroundFailures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AccountViewModelTest {

    @Test
    fun `signed out has no email`() = runTest {
        val viewModel = viewModel()

        val state = viewModel.uiState.first { !it.signingIn }

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
        val viewModel = viewModel(FakeAccountAuth(AccountUser("stolea@gmail.com")))

        viewModel.uiState.first { it.email == "stolea@gmail.com" }
        viewModel.onAction(AccountAction.OnSignOutClicked)
        assert(viewModel.uiState.first { it.confirmSignOut }.email == "stolea@gmail.com")

        viewModel.onAction(AccountAction.OnSignOutDismissed)
        val dismissed = viewModel.uiState.first { !it.confirmSignOut }
        assert(dismissed.email == "stolea@gmail.com")
    }

    @Test
    fun `confirming sign-out returns to signed out`() = runTest {
        val auth = FakeAccountAuth(AccountUser("stolea@gmail.com"))
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

    private fun kotlinx.coroutines.test.TestScope.viewModel(
        auth: FakeAccountAuth = FakeAccountAuth(),
        googleSignIn: FakeGoogleSignIn = FakeGoogleSignIn(),
        coroutineScope: CoroutineScope = backgroundScope,
    ) = AccountViewModel(auth, googleSignIn, coroutineScope)
}
