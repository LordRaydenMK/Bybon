package dev.sanastasov.bybon.test

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle

/**
 * Captures invariant failures thrown from a ViewModel `coroutineScope.launch`.
 *
 * [assertFailsWith] does not see those exceptions: [TestScope] reports them as
 * uncaught after the block returns, and [backgroundScope] swallows them. This
 * scope uses the test dispatcher with a supervisor job so the thrown
 * [IllegalStateException] can be asserted without cancelling `runTest`.
 */
class BackgroundFailures(
    private val testScope: TestScope,
) {
    private val failures = mutableListOf<Throwable>()
    private val job = SupervisorJob(testScope.backgroundScope.coroutineContext[Job])

    val scope: CoroutineScope = CoroutineScope(
        testScope.coroutineContext
            .minusKey(Job)
            .minusKey(CoroutineExceptionHandler) +
            job +
            CoroutineExceptionHandler { _, error -> failures += error },
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    fun expectFailure(expectedMessage: String, action: () -> Unit) {
        action()
        testScope.advanceUntilIdle()
        val error = failures.singleOrNull()
        checkNotNull(error) { "Expected a background failure, but none was recorded" }
        assert(error is IllegalStateException)
        assert(error.message == expectedMessage)
    }
}
