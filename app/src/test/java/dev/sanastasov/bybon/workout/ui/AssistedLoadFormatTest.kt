package dev.sanastasov.bybon.workout.ui

import dev.sanastasov.bybon.workout.domain.Weight
import org.junit.Test

class AssistedLoadFormatTest {

    @Test
    fun `adjusted load label is body weight then effective load`() {
        assert(
            formatAssistedAdjustedLoadLabel(Weight.kilograms(80), Weight.kilograms(55)) ==
                "80 → 55 kg",
        )
        assert(formatAssistedAdjustedLoadLabel(Weight.kilograms(80), null) == "80 kg")
        assert(formatAssistedAdjustedLoadLabel(null, Weight.kilograms(55)) == null)
        assert(formatLoggedLoadWithAdjusted("-25", "80 → 55 kg") == "-25 · 80 → 55 kg")
        assert(formatLoggedLoadWithAdjusted("0", null) == "0")
        assert(formatLoggedLoadWithAdjusted(null, null) == "-")
    }
}
