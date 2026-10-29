package dev.sanastasov.bybon.workout.ui

import dev.sanastasov.bybon.workout.domain.Weight
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
}
