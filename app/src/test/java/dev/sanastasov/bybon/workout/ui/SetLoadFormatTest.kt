package dev.sanastasov.bybon.workout.ui

import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.estimateOneRmKg
import org.junit.Test

class SetLoadFormatTest {

    @Test
    fun `1RM delta is dash when current matches previous`() {
        val oneRm = Weight.kilograms(63f)
        assert(formatOneRmDelta(oneRm, oneRm) == "-")
    }

    @Test
    fun `1RM delta is signed kilograms when current is higher`() {
        assert(
            formatOneRmDelta(Weight.kilograms(10.1f), Weight.kilograms(9f)) == "+1.1kg",
        )
    }

    @Test
    fun `1RM delta is signed kilograms when current is lower`() {
        assert(formatOneRmDelta(Weight.kilograms(9f), Weight.kilograms(10f)) == "-1kg")
    }

    @Test
    fun `1RM delta is dash when either estimate is missing`() {
        assert(formatOneRmDelta(null, Weight.kilograms(10f)) == "-")
        assert(formatOneRmDelta(Weight.kilograms(10f), null) == "-")
        assert(formatOneRmDelta(null, null) == "-")
    }

    @Test
    fun `1RM change uses an arrow and names no change`() {
        assert(
            formatOneRmChange(Weight.kilograms(26.25f), Weight.kilograms(25.67f)) ==
                "↑ 0.58 kg",
        )
        assert(
            formatOneRmChange(Weight.kilograms(24f), Weight.kilograms(25f)) ==
                "↓ 1 kg",
        )
        assert(formatOneRmChange(Weight.kilograms(25f), Weight.kilograms(25f)) == "No change")
        assert(formatOneRmChange(null, Weight.kilograms(25f)) == "-")
        assert(formatOneRmChange(Weight.kilograms(25f), null) == "-")
    }

    @Test
    fun `1RM change rounds estimated top sets the same way as the summary`() {
        val current = Weight.kilograms(estimateOneRmKg(20f, 14))
        val previous = Weight.kilograms(estimateOneRmKg(20f, 13))
        assert(formatOneRmChange(current, previous) == "↑ 0.66 kg")
    }
}
