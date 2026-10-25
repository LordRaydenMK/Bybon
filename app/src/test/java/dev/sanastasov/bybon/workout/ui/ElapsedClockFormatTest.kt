package dev.sanastasov.bybon.workout.ui

import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

class ElapsedClockFormatTest {

    @Test
    fun `formatElapsedClock pads hours and minutes`() {
        assert(10.seconds.formatElapsedClock() == "00:00")
        assert(0.seconds.formatElapsedClock() == "00:00")
        assert(1.minutes.formatElapsedClock() == "00:01")
        assert((47.minutes + 44.seconds).formatElapsedClock() == "00:47")
        assert((50.minutes + 20.seconds).formatElapsedClock() == "00:50")
        assert(1.hours.formatElapsedClock() == "01:00")
        assert((1.hours + 5.minutes).formatElapsedClock() == "01:05")
        assert(125.minutes.formatElapsedClock() == "02:05")
    }
}
