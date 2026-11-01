package dev.sanastasov.bybon.workout

import kotlin.time.Duration

interface RestPromoteScheduler {
    fun schedule(wait: Duration)
    fun cancel()

    companion object {
        val NoOp: RestPromoteScheduler = object : RestPromoteScheduler {
            override fun schedule(wait: Duration) = Unit
            override fun cancel() = Unit
        }
    }
}
