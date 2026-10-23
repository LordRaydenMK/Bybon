package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

class ElapsedClockTest {

    @Test
    fun `formatElapsedClock pads minutes and seconds`() {
        assert(10.seconds.formatElapsedClock() == "00:10")
        assert((50.minutes + 20.seconds).formatElapsedClock() == "50:20")
        assert(0.seconds.formatElapsedClock() == "00:00")
        assert(1.seconds.formatElapsedClock() == "00:01")
        assert(1.minutes.formatElapsedClock() == "01:00")
        assert(125.minutes.formatElapsedClock() == "125:00")
    }

    @Test
    fun `elapsedClockContentDescription names minutes and seconds`() {
        assert(0.seconds.elapsedClockContentDescription() == "Elapsed 0 seconds")
        assert(1.seconds.elapsedClockContentDescription() == "Elapsed 1 second")
        assert(10.seconds.elapsedClockContentDescription() == "Elapsed 10 seconds")
        assert(1.minutes.elapsedClockContentDescription() == "Elapsed 1 minute")
        assert(
            (1.minutes + 1.seconds).elapsedClockContentDescription() ==
                "Elapsed 1 minute 1 second",
        )
        assert(
            (50.minutes + 20.seconds).elapsedClockContentDescription() ==
                "Elapsed 50 minutes 20 seconds",
        )
    }

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
