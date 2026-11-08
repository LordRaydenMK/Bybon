package dev.sanastasov.bybon.bodyweight.sync

const val IMMEDIATE_WEIGHT_SYNC = "weight-sync"
const val DAILY_WEIGHT_SYNC = "weight-sync-daily"

data class WeightSyncWorkSpec(
    val uniqueName: String,
    val signIn: Boolean,
    val periodic: Boolean,
    val networkRequired: Boolean,
)

fun immediateWeightSyncSpec(signIn: Boolean): WeightSyncWorkSpec = WeightSyncWorkSpec(
    uniqueName = IMMEDIATE_WEIGHT_SYNC,
    signIn = signIn,
    periodic = false,
    networkRequired = false,
)

fun dailyWeightSyncSpec(): WeightSyncWorkSpec = WeightSyncWorkSpec(
    uniqueName = DAILY_WEIGHT_SYNC,
    signIn = false,
    periodic = true,
    networkRequired = true,
)

interface WeightSyncScheduler {
    fun enqueueImmediate(signIn: Boolean = false)

    fun ensureDaily()
}
