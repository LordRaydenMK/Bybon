package dev.sanastasov.bybon.workout.ui.summary

import dev.sanastasov.bybon.workout.domain.Weight
import org.junit.Test

class WorkoutSummaryUiTest {

    @Test
    fun `work set content description names load and e1RM`() {
        val set = WorkoutSummarySetUi(1, "20", 14, Weight.kilograms(29.33f))
        assert(set.contentDescription() == "1. 20 kg × 14, 29.33 kg e1RM")
    }

    @Test
    fun `missing load content description uses dashes`() {
        val set = WorkoutSummarySetUi(1, "-", 8, null)
        assert(set.contentDescription() == "1. - × 8, -")
    }

    @Test
    fun `summary load uses times sign and kilograms`() {
        assert(formatSummaryLoad("17.5", 15) == "17.5 kg × 15")
        assert(formatSummaryLoad("-", 8) == "- × 8")
    }

    @Test
    fun `vs previous uses an arrow and hides an unchanged estimate`() {
        assert(
            formatOneRmVsPrevious(Weight.kilograms(26.25f), Weight.kilograms(25.67f)) ==
                "↑ 0.58 kg vs previous",
        )
        assert(
            formatOneRmVsPrevious(Weight.kilograms(24f), Weight.kilograms(25f)) ==
                "↓ 1 kg vs previous",
        )
        assert(formatOneRmVsPrevious(Weight.kilograms(25f), Weight.kilograms(25f)) == null)
        assert(formatOneRmVsPrevious(Weight.kilograms(25f), null) == null)
    }
}
