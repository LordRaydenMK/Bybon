package dev.sanastasov.bybon.workout.ui

import dev.sanastasov.bybon.workout.domain.ExerciseSet
import dev.sanastasov.bybon.workout.domain.NumberedSet
import dev.sanastasov.bybon.workout.domain.PreviousSetPerformance
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.catalogExercise
import org.junit.Test

class NumberedSetExtTest {

    private val bench = catalogExercise("bench-press")

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
        assert(numbered.warmupSetNumber == null)
        assert(numbered.setNumber == 1)
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
    fun `missing load renders as dash and has no 1RM`() {
        val numbered = NumberedSet(
            set = ExerciseSet(
                catalogExercise("assisted-pull-ups"),
                null,
                8,
                SetState.Completed(),
            ),
            isWarmup = false,
            index = 0,
        )

        assert(numbered.oneRmLabel == "-")
        assert(numbered.weightRepsLabel == "- x 8")
        assert(numbered.overviewContentDescription == "Set 1, - by 8, -")
        assert(numbered.completedContentDescription == "1. - x 8 @ -")
    }

    @Test
    fun `in-progress warmup description omits 1RM`() {
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

        assert(numbered.warmupSetNumber == 1)
        assert(numbered.workSetNumber == null)
        assert(numbered.setNumber == 1)
        assert(numbered.setTitle == "Warmup set")
        assert(numbered.sessionContentDescription == "Warmup set in progress, 20 kg by 8")
        assert(numbered.completeSetContentDescription == "Mark Warmup set complete")
        assert(numbered.weightFieldContentDescription == "Warmup set weight")
        assert(numbered.repsFieldContentDescription == "Warmup set reps")
        assert(numbered.overviewContentDescription == "Warmup set, 20 kg by 8")
    }

    @Test
    fun `in-progress work set complete action names the set number`() {
        val numbered = NumberedSet(
            set = ExerciseSet(
                bench,
                Weight.kilograms(50),
                8,
                SetState.InProgress,
            ),
            isWarmup = false,
            index = 0,
        )

        assert(
            numbered.sessionContentDescription ==
                "Set 1 in progress, 50 kg by 8, @ 62.07 kg 1RM",
        )
        assert(numbered.completeSetContentDescription == "Mark Set 1 complete")
        assert(numbered.weightFieldContentDescription == "Set 1 weight")
        assert(numbered.repsFieldContentDescription == "Set 1 reps")
    }

    @Test
    fun `warmup notification text prefixes w and uses weight times reps`() {
        val numbered = NumberedSet(
            set = ExerciseSet(
                bench,
                Weight.kilograms(40),
                8,
                SetState.InProgress,
            ),
            isWarmup = true,
            index = 0,
        )

        assert(numbered.notificationText(2) == "[w] 1/2 Barbell Bench Press 40 kg x 8")
    }

    @Test
    fun `second warmup notification text uses warmup index over warmup count`() {
        val numbered = NumberedSet(
            set = ExerciseSet(
                bench,
                Weight.kilograms(40),
                4,
                SetState.InProgress,
            ),
            isWarmup = true,
            index = 1,
        )

        assert(numbered.setNumber == 2)
        assert(numbered.notificationText(3) == "[w] 2/3 Barbell Bench Press 40 kg x 4")
    }

    @Test
    fun `work notification text uses exercise name and weight times reps`() {
        val numbered = NumberedSet(
            set = ExerciseSet(
                bench,
                Weight.kilograms(50),
                9,
                SetState.InProgress,
            ),
            isWarmup = false,
            index = 0,
        )

        assert(numbered.notificationText(3) == "1/3 Barbell Bench Press 50 kg x 9")
    }

    @Test
    fun `second work notification text uses set index over work count`() {
        val numbered = NumberedSet(
            set = ExerciseSet(
                bench,
                Weight.kilograms(50),
                9,
                SetState.InProgress,
            ),
            isWarmup = false,
            index = 1,
        )

        assert(numbered.setNumber == 2)
        assert(numbered.notificationText(3) == "2/3 Barbell Bench Press 50 kg x 9")
    }

    @Test
    fun `notification text uses dash when load is missing`() {
        val pullUp = catalogExercise("assisted-pull-ups")
        val numbered = NumberedSet(
            set = ExerciseSet(
                pullUp,
                null,
                8,
                SetState.InProgress,
            ),
            isWarmup = false,
            index = 0,
        )

        assert(numbered.notificationText(3) == "1/3 ${pullUp.name} - x 8")
    }
}
