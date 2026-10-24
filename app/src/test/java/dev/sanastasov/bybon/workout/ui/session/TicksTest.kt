package dev.sanastasov.bybon.workout.ui.session

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TicksTest {

    @Test
    fun `takeUntil emits the matching item then stops`() = runTest {
        val values = flowOf(1, 2, 3, 4).takeUntil { it == 3 }.toList()
        assert(values == listOf(1, 2, 3))
    }

    @Test
    fun `takeUntil emits a terminal null`() = runTest {
        val values = flowOf("a", "b", null, "c").takeUntil { it == null }.toList()
        assert(values == listOf("a", "b", null))
    }
}
