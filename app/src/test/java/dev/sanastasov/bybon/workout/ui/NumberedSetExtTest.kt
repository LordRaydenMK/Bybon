package dev.sanastasov.bybon.workout.ui

import dev.sanastasov.bybon.workout.domain.ExerciseSet
import dev.sanastasov.bybon.workout.domain.NumberedSet
import dev.sanastasov.bybon.workout.domain.PreviousSetPerformance
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.catalogExercise
import org.junit.Test

class NumberedSetExtTest {

    private val bench = catalogExercise("bench-press-bb")

    @Test
    fun `work set labels use set number and 1RM`() {
        val numbered = NumberedSet(
            set = ExerciseSet(
                bench,
                Weight.kilograms(50),
                8,
                SetState.NotStated,
                PreviousSetPerformance(Weight.kilograms(45), 9),
            ),
            isWarmup = false,
            index = 0,
        )

        assert(numbered.workSetNumber == 1)
        assert(numbered.setTitle == "Set 1")
        assert(numbered.oneRmLabel == "@ 62.07 kg 1RM")
        assert(numbered.previousLabel == "45 x 9")
        assert(
            numbered.overviewContentDescription ==
                "Set 1, 50 kg by 8, @ 62.07 kg 1RM. Previous: 45 x 9",
        )
        assert(numbered.completedContentDescription == "1. 50 kg x 8 @ 62.07 kg 1RM")
    }

    @Test
    fun `in-progress warmup description omits 1RM and includes complete hint`() {
        val numbered = NumberedSet(
            set = ExerciseSet(
                bench,
                Weight.kilograms(20),
                8,
                SetState.InProgress,
            ),
            isWarmup = true,
            index = 0,
        )

        assert(numbered.setTitle == "Warmup set")
        assert(
            numbered.sessionContentDescription ==
                "Warmup set in progress, 20 kg by 8. Double tap to mark complete.",
        )
        assert(numbered.overviewContentDescription == "Warmup set, 20 kg by 8")
    }
}
