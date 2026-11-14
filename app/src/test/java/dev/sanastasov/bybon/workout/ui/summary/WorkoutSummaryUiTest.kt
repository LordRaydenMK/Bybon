package dev.sanastasov.bybon.workout.ui.summary

import dev.sanastasov.bybon.workout.domain.Weight
import org.junit.Test

class WorkoutSummaryUiTest {

    @Test
    fun `session summary content description names total weight`() {
        val summary = WorkoutSessionSummaryUi(
            subtitle = "Full Body B · 13 Aug 2026 · Start 18:25",
            duration = "47m",
            exerciseCount = 2,
            workingSetCount = 5,
            volumeLoadLabel = "1762.5 kg",
        )
        assert(
            summary.contentDescription() ==
                "Workout completed, Full Body B · 13 Aug 2026 · Start 18:25, " +
                "duration 47m, 2 exercises, 5 working sets, 1762.5 kg total weight",
        )
    }

    @Test
    fun `work set content description names load and e1RM`() {
        val set = WorkoutSummarySetUi(1, "20", 14, Weight.kilograms(29.33f))
        assert(set.contentDescription() == "1. 20 kg × 14, 29.33 kg e1RM")
    }

    @Test
    fun `missing load content description uses dashes`() {
        val set = WorkoutSummarySetUi(1, null, 8, null)
        assert(set.contentDescription() == "1. - × 8, -")
    }

    @Test
    fun `summary load uses times sign and kilograms`() {
        assert(formatSummaryLoad("17.5", 15) == "17.5 kg × 15")
        assert(formatSummaryLoad(null, 8) == "- × 8")
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
    }

    @Test
    fun `average e1RM change omits vs previous and names no change`() {
        assert(
            formatAverageOneRmChange(Weight.kilograms(26.25f), Weight.kilograms(25.67f)) ==
                "↑ 0.58 kg",
        )
        assert(
            formatAverageOneRmChange(Weight.kilograms(24f), Weight.kilograms(25f)) ==
                "↓ 1 kg",
        )
        assert(
            formatAverageOneRmChange(Weight.kilograms(25f), Weight.kilograms(25f)) ==
                "No change",
        )
    }
}
