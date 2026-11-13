package dev.sanastasov.bybon.ui

import androidx.lifecycle.SavedStateHandle
import dev.marcellogalhardo.retained.core.RetainedEntry
import kotlin.coroutines.ContinuationInterceptor
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import org.junit.Test

class RetainedScopeTest {

    @Test
    fun `default scope uses Default and the retained job`() {
        val job = SupervisorJob()
        val entry = object : RetainedEntry {
            override val key = "scope"
            override val retainedClass: KClass<out Any> = String::class
            override val coroutineScope = CoroutineScope(job + Dispatchers.Unconfined)
            override val savedStateHandle = SavedStateHandle()
        }

        val scope = entry.defaultScope()

        assert(scope.coroutineContext[ContinuationInterceptor] == Dispatchers.Default)
        assert(scope.coroutineContext[Job] === job)
    }
}
