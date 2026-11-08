package dev.sanastasov.bybon.workout.domain

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import java.time.LocalDate
import org.junit.Test

class SevenDayBodyWeightAverageTest {

    @Test
    fun `seven day average needs three logs in the window`() {
        val on = LocalDate.of(2026, 11, 8)
        val two = listOf(
            BodyWeightEntry(on.minusDays(1), BodyWeight.parseFromString("80")),
            BodyWeightEntry(on, BodyWeight.parseFromString("81")),
        )
        val three = two + BodyWeightEntry(on.minusDays(6), BodyWeight.parseFromString("82"))
        val outside = three + BodyWeightEntry(on.minusDays(7), BodyWeight.parseFromString("90"))

        assert(sevenDayBodyWeightAverage(two, on) == null)
        assert(sevenDayBodyWeightAverage(three, on) == Weight.kilograms(81f))
        assert(sevenDayBodyWeightAverage(outside, on) == Weight.kilograms(81f))
    }
}
