package dev.sanastasov.bybon.workout.ui.summary

import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.ui.details.PersonalRecordUi
import org.junit.Test

class WorkoutSummaryUiTest {

    @Test
    fun `work set content description names load and 1RM without at-sign`() {
        val set = WorkoutSummarySetUi(1, "20", 14, Weight.kilograms(29.33f))
        assert(set.contentDescription() == "1. 20 kg x 14, 29.33 kg 1RM, -")
    }

    @Test
    fun `missing load content description uses dashes`() {
        val set = WorkoutSummarySetUi(1, "-", 8, null)
        assert(set.contentDescription() == "1. - x 8, -, -")
    }

    @Test
    fun `work set content description includes 1RM delta vs previous`() {
        val set = WorkoutSummarySetUi(
            1,
            "45",
            12,
            Weight.kilograms(63f),
            previousOneRm = Weight.kilograms(57.86f),
        )
        assert(set.contentDescription() == "1. 45 kg x 12, 63 kg 1RM, +5.14kg")
    }

    @Test
    fun `all-time 1RM content description includes weight and date`() {
        val record = PersonalRecordUi(Weight.kilograms(78f), "20 Sep 2026")
        assert(record.contentDescription() == "All-time 1RM 78 kg, 20 Sep 2026")
    }
}
