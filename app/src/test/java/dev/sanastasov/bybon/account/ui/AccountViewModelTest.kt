package dev.sanastasov.bybon.account.ui

import dev.sanastasov.bybon.account.FakeAccountAuth
import dev.sanastasov.bybon.account.FakeGoogleSignIn
import dev.sanastasov.bybon.account.domain.AccountUser
import dev.sanastasov.bybon.bodyweight.sync.FakeWeightBackup
import dev.sanastasov.bybon.bodyweight.sync.RecordingWeightSyncScheduler
import dev.sanastasov.bybon.bodyweight.sync.WeightBackupStatus
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
    fun `opening account while signed in syncs without enqueueing another job`() = runTest {
        val backup = FakeWeightBackup()
        val scheduler = RecordingWeightSyncScheduler()
        val viewModel = viewModel(
            auth = FakeAccountAuth(AccountUser("uid", "stolea@gmail.com")),
            backup = backup,
            scheduler = scheduler,
        )

        viewModel.uiState.first { it.email == "stolea@gmail.com" }
        yield()

        assert(backup.syncs.single() == ("uid" to false))
        assert(scheduler.immediate.isEmpty())
    }

    @Test
    fun `sign-in enqueues one immediate full sync`() = runTest {
        val backup = FakeWeightBackup()
        val scheduler = RecordingWeightSyncScheduler()
        val viewModel = viewModel(backup = backup, scheduler = scheduler)

        viewModel.uiState.first { !it.signingIn }
        viewModel.onAction(AccountAction.OnSignInClicked)
        viewModel.uiState.first { it.email == "token@example.com" }
        yield()

        assert(backup.syncs.any { it == ("token" to true) })
        assert(scheduler.immediate == listOf(true))
    }

    private fun kotlinx.coroutines.test.TestScope.viewModel(
        auth: FakeAccountAuth = FakeAccountAuth(),
        googleSignIn: FakeGoogleSignIn = FakeGoogleSignIn(),
        environmentName: String = "prd",
        backup: FakeWeightBackup = FakeWeightBackup(),
        scheduler: RecordingWeightSyncScheduler = RecordingWeightSyncScheduler(),
    ) = AccountViewModel(
        auth,
        googleSignIn,
        environmentName,
        backup,
        scheduler,
        backgroundScope,
    )
}
