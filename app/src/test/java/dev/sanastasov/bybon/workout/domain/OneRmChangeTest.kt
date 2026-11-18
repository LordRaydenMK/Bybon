package dev.sanastasov.bybon.workout.domain

import org.junit.Test

class OneRmChangeTest {

    @Test
    fun `oneRmChange is improved when the new estimate is higher`() {
        val set = benchSet(50f, 10, previous = PreviousSetPerformance(Weight.kilograms(45), 8))

        assert(set.oneRmChange() == OneRmChange.Improved)
    }

    @Test
    fun `oneRmChange is decreased when the new estimate is lower`() {
        val set = benchSet(45f, 8, previous = PreviousSetPerformance(Weight.kilograms(45), 10))

        assert(set.oneRmChange() == OneRmChange.Decreased)
    }

    @Test
    fun `oneRmChange is unchanged when the estimates match`() {
        val previous = PreviousSetPerformance(Weight.kilograms(45), 9)

        assert(benchSet(45f, 9, previous).oneRmChange() == OneRmChange.Unchanged)
    }

    @Test
    fun `oneRmChange is absent when either estimate is missing`() {
        val previous = PreviousSetPerformance(Weight.kilograms(45), 9)

        assert(benchSet(weight = null, reps = 8, previous = previous).oneRmChange() == null)
        assert(benchSet(45f, 9, previous = null).oneRmChange() == null)
    }

    private fun benchSet(
        weight: Float?,
        reps: Int,
        previous: PreviousSetPerformance?,
    ): ExerciseSet = ExerciseSet(
        catalogExercise("bench-press"),
        weight?.let(Weight::kilograms),
        reps,
        SetState.NotStated,
        previous = previous,
    )
}
