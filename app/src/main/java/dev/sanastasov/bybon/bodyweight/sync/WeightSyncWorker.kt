package dev.sanastasov.bybon.bodyweight.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.sanastasov.bybon.BybonApplication
import dev.sanastasov.bybon.account.domain.AccountUser
import kotlinx.coroutines.flow.first

class WeightSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val module = (applicationContext as BybonApplication).mainModule
        runWeightSync(
            user = module.accountAuth.user().first(),
            signIn = inputData.getBoolean(KEY_SIGN_IN, false),
            databaseName = module.firebaseEnvName,
            backup = module.weightBackup,
        )
        return Result.success()
    }

    companion object {
        const val KEY_SIGN_IN = "signIn"
    }
}

suspend fun runWeightSync(
    user: AccountUser?,
    signIn: Boolean,
    databaseName: String,
    backup: WeightBackup,
) {
    val signedIn = user ?: return
    backup.sync(databaseName, signedIn.uid, signIn)
}
