package dev.sanastasov.bybon.bodyweight.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class WorkManagerWeightSyncScheduler(
    context: Context,
    private val workManager: WorkManager = WorkManager.getInstance(context),
) : WeightSyncScheduler {

    override fun enqueueImmediate(signIn: Boolean) {
        val spec = immediateWeightSyncSpec(signIn)
        val request = OneTimeWorkRequestBuilder<WeightSyncWorker>()
            .setInputData(workDataOf(WeightSyncWorker.KEY_SIGN_IN to spec.signIn))
            .build()
        workManager.enqueueUniqueWork(
            spec.uniqueName,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request,
        )
    }

    override fun ensureDaily() {
        val spec = dailyWeightSyncSpec()
        val request = PeriodicWorkRequestBuilder<WeightSyncWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .setInputData(workDataOf(WeightSyncWorker.KEY_SIGN_IN to spec.signIn))
            .build()
        workManager.enqueueUniquePeriodicWork(
            spec.uniqueName,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
