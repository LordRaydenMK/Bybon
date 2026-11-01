package dev.sanastasov.bybon.workout.ui.history

import dev.sanastasov.bybon.workout.domain.Weight
import org.junit.Test

class WorkoutHistoryUiTest {

    @Test
    fun `top set content description names load and 1RM`() {
        val topSet = ExerciseTopSetUi(
            name = "Romanian Deadlift",
            weightKg = "45",
            reps = 12,
            oneRm = Weight.kilograms(63f),
        )
        assert(topSet.contentDescription() == "Romanian Deadlift, 45 kg x 12, 63 kg 1RM")
    }

    @Test
    fun `missing load content description uses dashes`() {
        val topSet = ExerciseTopSetUi(
            name = "Assisted Pull-up",
            weightKg = "-",
            reps = 8,
            oneRm = null,
        )
        assert(topSet.contentDescription() == "Assisted Pull-up, - x 8, -")
    }
}
