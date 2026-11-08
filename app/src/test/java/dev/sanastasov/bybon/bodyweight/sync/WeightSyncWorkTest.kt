package dev.sanastasov.bybon.bodyweight.sync

import dev.sanastasov.bybon.account.domain.AccountUser
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WeightSyncWorkTest {

    @Test
    fun `a weight change and sign-in enqueue the same immediate job`() {
        val change = immediateWeightSyncSpec(signIn = false)
        val signIn = immediateWeightSyncSpec(signIn = true)

        assert(change.uniqueName == signIn.uniqueName)
        assert(change.uniqueName == IMMEDIATE_WEIGHT_SYNC)
        assert(!change.periodic)
        assert(!change.signIn)
        assert(signIn.signIn)
        assert(!change.networkRequired)
    }

    @Test
    fun `the daily job is the cursor path and requires a network`() {
        val daily = dailyWeightSyncSpec()

        assert(daily.uniqueName == DAILY_WEIGHT_SYNC)
        assert(daily.periodic)
        assert(!daily.signIn)
        assert(daily.networkRequired)
    }

    @Test
    fun `a signed-out sync does nothing`() = runTest {
        val backup = FakeWeightBackup()

        runWeightSync(user = null, signIn = false, databaseName = "stg", backup = backup)

        assert(backup.syncs.isEmpty())
    }

    @Test
    fun `a signed-in daily sync does not force a full read`() = runTest {
        val backup = FakeWeightBackup()

        runWeightSync(
            user = AccountUser("uid", "stolea@gmail.com"),
            signIn = dailyWeightSyncSpec().signIn,
            databaseName = "stg",
            backup = backup,
        )

        assert(backup.syncs.single() == ("uid" to false))
    }
}
