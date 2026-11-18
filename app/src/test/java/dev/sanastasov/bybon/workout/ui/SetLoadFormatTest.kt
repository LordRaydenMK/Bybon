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

    @Test
    fun `1RM change is improved when the new estimate is higher`() {
        assert(
            oneRmChange(Weight.kilograms(10.1f), Weight.kilograms(9f)) == OneRmChange.Improved,
        )
    }

    @Test
    fun `1RM change is decreased when the new estimate is lower`() {
        assert(oneRmChange(Weight.kilograms(9f), Weight.kilograms(10f)) == OneRmChange.Decreased)
    }

    @Test
    fun `1RM change is unchanged when estimates match or are missing`() {
        val oneRm = Weight.kilograms(63f)
        assert(oneRmChange(oneRm, oneRm) == OneRmChange.Unchanged)
        assert(oneRmChange(null, oneRm) == OneRmChange.Unchanged)
        assert(oneRmChange(oneRm, null) == OneRmChange.Unchanged)
        assert(oneRmChange(null, null) == OneRmChange.Unchanged)
    }
}
