package dev.sanastasov.bybon.workout.domain

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import java.time.LocalDate
import org.junit.Test

class SevenDayBodyWeightAverageTest {

    private val on = LocalDate.of(2026, 11, 8)

    @Test
    fun `returns null with fewer than three logs in the window`() {
        val entries = listOf(
            BodyWeightEntry(on.minusDays(1), BodyWeight.parseFromString("80")),
            BodyWeightEntry(on, BodyWeight.parseFromString("81")),
        )

        assert(sevenDayBodyWeightAverage(entries, on) == null)
    }

    @Test
    fun `averages three logs in the window`() {
        val entries = listOf(
            BodyWeightEntry(on.minusDays(1), BodyWeight.parseFromString("80")),
            BodyWeightEntry(on, BodyWeight.parseFromString("81")),
            BodyWeightEntry(on.minusDays(6), BodyWeight.parseFromString("82")),
        )

        assert(sevenDayBodyWeightAverage(entries, on) == Weight.kilograms(81f))
    }

    @Test
    fun `ignores logs outside the window`() {
        val entries = listOf(
            BodyWeightEntry(on.minusDays(1), BodyWeight.parseFromString("80")),
            BodyWeightEntry(on, BodyWeight.parseFromString("81")),
            BodyWeightEntry(on.minusDays(6), BodyWeight.parseFromString("82")),
            BodyWeightEntry(on.minusDays(7), BodyWeight.parseFromString("90")),
        )

        assert(sevenDayBodyWeightAverage(entries, on) == Weight.kilograms(81f))
    }
}
