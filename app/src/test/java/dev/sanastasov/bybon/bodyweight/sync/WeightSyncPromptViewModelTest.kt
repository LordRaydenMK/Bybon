package dev.sanastasov.bybon.bodyweight.sync

import dev.sanastasov.bybon.account.FakeAccountAuth
import dev.sanastasov.bybon.account.FakeGoogleSignIn
import dev.sanastasov.bybon.account.domain.AccountUser
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WeightSyncPromptViewModelTest {

    @Test
    fun `a stored conflict shows the three choices`() = runTest {
        val backup = FakeWeightBackup()
        backup.status.value = WeightBackupStatus(conflict = true)
        val viewModel = viewModel(backup)

        val prompt = viewModel.prompt.first { it is WeightSyncPrompt.Conflict }

        assert(
            prompt.conflictChoices == listOf(
                WeightSyncPromptAction.UseTheBackup,
                WeightSyncPromptAction.ReplaceTheBackup,
                WeightSyncPromptAction.KeepThisPhone,
            ),
        )
    }

    @Test
    fun `a stored summary shows once`() = runTest {
        val backup = FakeWeightBackup()
        backup.status.value = WeightBackupStatus(
            summary = WeightImportSummary(
                count = 2,
                earliest = LocalDate.of(2026, 1, 1),
                latest = LocalDate.of(2026, 2, 1),
            ),
        )
        val viewModel = viewModel(backup)

        val showing = viewModel.prompt.first { it is WeightSyncPrompt.Summary }
        val summary = (showing as WeightSyncPrompt.Summary).summary
        assert(summary.count == 2)

        viewModel.onAction(WeightSyncPromptAction.DismissSummary)
        val dismissed = viewModel.prompt.first { it is WeightSyncPrompt.None }
        assert(dismissed is WeightSyncPrompt.None)
    }

    @Test
    fun `keep this phone signs out and leaves the question`() = runTest {
        val auth = FakeAccountAuth(AccountUser("uid", "stolea@gmail.com"))
        val backup = FakeWeightBackup()
        backup.status.value = WeightBackupStatus(conflict = true)
        val viewModel = viewModel(backup, auth)

        viewModel.prompt.first { it is WeightSyncPrompt.Conflict }
        viewModel.onAction(WeightSyncPromptAction.KeepThisPhone)

        val cleared = viewModel.prompt.first { it is WeightSyncPrompt.None }
        assert(cleared is WeightSyncPrompt.None)
        assert(auth.signedOut)
        assert(backup.kept)
    }

    private fun kotlinx.coroutines.test.TestScope.viewModel(
        backup: FakeWeightBackup,
        auth: FakeAccountAuth = FakeAccountAuth(),
    ) = WeightSyncPromptViewModel(
        backup,
        auth,
        FakeGoogleSignIn(),
        "stg",
        backgroundScope,
    )
}
