package dev.sanastasov.bybon.workout.ui

import dev.sanastasov.bybon.workout.domain.Weight
import org.junit.Test

class SetLoadOneRmTableTest {

    @Test
    fun `load and 1RM description has no at-sign`() {
        assert(
            setLoadOneRmDescription("20", 14, Weight.kilograms(29.33f)) ==
                "20 kg x 14, 29.33 kg 1RM",
        )
    }

    @Test
    fun `missing load description uses dashes`() {
        assert(setLoadOneRmDescription("-", 8, null) == "- x 8, -")
    }
}
