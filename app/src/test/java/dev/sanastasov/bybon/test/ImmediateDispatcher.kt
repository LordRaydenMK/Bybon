package dev.sanastasov.bybon.test

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Runs dispatched work on the caller thread while still switching
 * [kotlinx.coroutines.withContext] / [kotlinx.coroutines.flow.flowOn] onto this interceptor.
 */
class ImmediateDispatcher : CoroutineDispatcher() {
    var dispatchCount: Int = 0
        private set

    override fun isDispatchNeeded(context: CoroutineContext): Boolean = true

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        dispatchCount++
        block.run()
    }
}
