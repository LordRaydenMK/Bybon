package dev.sanastasov.bybon.workout.ui.session

import dev.sanastasov.bybon.workout.domain.RestTimer
import dev.sanastasov.bybon.workout.ui.toStartTimeLabel
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

class SessionElapsedClockTest {

    @Test
    fun `start time label uses 24-hour clock`() {
        assert(LocalDateTime.of(2026, 8, 20, 17, 59, 34).toStartTimeLabel() == "17:59")
        assert(LocalDateTime.of(2026, 8, 13, 18, 0).toStartTimeLabel() == "18:00")
        assert(LocalDateTime.of(2026, 8, 13, 9, 5).toStartTimeLabel() == "09:05")
    }

    @Test
    fun `formatSessionElapsedClock pads minutes and seconds`() {
        assert(10.seconds.formatSessionElapsedClock() == "00:10")
        assert((50.minutes + 20.seconds).formatSessionElapsedClock() == "50:20")
        assert(0.seconds.formatSessionElapsedClock() == "00:00")
        assert(1.seconds.formatSessionElapsedClock() == "00:01")
        assert(1.minutes.formatSessionElapsedClock() == "01:00")
        assert(125.minutes.formatSessionElapsedClock() == "125:00")
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
    fun `rest progress omits the bar when remaining is zero`() {
        val timer = RestTimer(LocalDateTime.of(2026, 10, 24, 12, 0), 2.minutes)
        assert(timer.progressUi(Duration.ZERO) == null)
        assert(timer.progressUi(75.seconds) == RestProgressUi(0.375f, 75.seconds))
    }

    @Test
    fun `restRemainingContentDescription names remaining minutes and seconds`() {
        assert(0.seconds.restRemainingContentDescription() == "Rest remaining 0 seconds")
        assert(1.seconds.restRemainingContentDescription() == "Rest remaining 1 second")
        assert(1.minutes.restRemainingContentDescription() == "Rest remaining 1 minute")
        assert(
            (1.minutes + 23.seconds).restRemainingContentDescription() ==
                "Rest remaining 1 minute 23 seconds",
        )
    }
}
