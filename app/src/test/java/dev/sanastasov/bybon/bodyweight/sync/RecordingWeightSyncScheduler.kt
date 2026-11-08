package dev.sanastasov.bybon.bodyweight.sync

class RecordingWeightSyncScheduler : WeightSyncScheduler {
    val immediate = mutableListOf<Boolean>()
    var dailyCount = 0
        private set

    override fun enqueueImmediate(signIn: Boolean) {
        immediate += signIn
    }

    override fun ensureDaily() {
        dailyCount += 1
    }
}
