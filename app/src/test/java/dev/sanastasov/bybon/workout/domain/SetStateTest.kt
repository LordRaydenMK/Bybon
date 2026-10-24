package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime
import kotlin.test.assertFailsWith
import org.junit.Test

class SetStateTest {

    @Test
    fun `fromStored reconstructs completed with a timestamp`() {
        val at = LocalDateTime.of(2026, 10, 24, 12, 0)
        assert(SetState.fromStored("Completed", at) == SetState.Completed(at))
        assert(SetState.fromStored("Completed", null) == SetState.Completed())
        assert(SetState.fromStored("InProgress", at) == SetState.InProgress)
        assert(SetState.fromStored("NotStated", null) == SetState.NotStated)
    }

    @Test
    fun `storedName is the enum-style column value`() {
        assert(SetState.NotStated.storedName == "NotStated")
        assert(SetState.InProgress.storedName == "InProgress")
        assert(SetState.Completed().storedName == "Completed")
        assert(SetState.Completed(LocalDateTime.of(2026, 10, 24, 12, 0)).storedName == "Completed")
    }

    @Test
    fun `fromStored rejects an unknown name`() {
        val error = assertFailsWith<IllegalStateException> {
            SetState.fromStored("Done", null)
        }
        assert(error.message == "Unknown set state Done")
    }
}
