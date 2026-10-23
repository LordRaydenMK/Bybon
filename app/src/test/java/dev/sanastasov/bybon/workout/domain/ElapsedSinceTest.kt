package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

class ElapsedSinceTest {

    @Test
    fun `elapsedSince is wall clock from startedAt`() {
        val startedAt = LocalDateTime.of(2026, 10, 23, 12, 0, 0)
        assert(elapsedSince(startedAt, startedAt.plusSeconds(10)) == 10.seconds)
        assert(
            elapsedSince(startedAt, startedAt.plusMinutes(50).plusSeconds(20)) ==
                50.minutes + 20.seconds,
        )
        assert(elapsedSince(startedAt, startedAt.minusSeconds(5)) == 0.seconds)
    }
}
