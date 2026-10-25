package dev.sanastasov.bybon.workout.ui.summary

import dev.sanastasov.bybon.workout.domain.Weight
import org.junit.Test

class WorkoutSummaryUiTest {

    @Test
    fun `work set content description names load and 1RM without at-sign`() {
        val set = WorkoutSummarySetUi(1, "20", 14, Weight.kilograms(29.33f))
        assert(set.contentDescription() == "1. 20 kg x 14, 29.33 kg 1RM")
    }

    @Test
    fun `missing load content description uses dashes`() {
        val set = WorkoutSummarySetUi(1, "-", 8, null)
        assert(set.contentDescription() == "1. - x 8, -")
    }
}
