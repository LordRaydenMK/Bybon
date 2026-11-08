package dev.sanastasov.bybon.workout.domain

import org.junit.Test

class AssistedOneRmTest {

    private val pullUp = catalogExercise("assisted-pull-ups")
    private val bodyWeight = Weight.kilograms(80)

    @Test
    fun `assisted 1RM uses body weight minus assistance`() {
        val set = ExerciseSet(
            pullUp,
            Weight.kilograms(25),
            8,
            SetState.Completed(),
            bodyWeight = bodyWeight,
        )

        assert(set.oneRm == Weight.kilograms(estimateOneRmKg(55f, 8)))
    }

    @Test
    fun `completed assisted set with no logged assist uses full body weight`() {
        val set = ExerciseSet(
            pullUp,
            null,
            8,
            SetState.Completed(),
            bodyWeight = bodyWeight,
        )

        assert(set.oneRm == Weight.kilograms(estimateOneRmKg(80f, 8)))
    }

    @Test
    fun `live assisted set with no logged assist has no 1RM`() {
        val set = ExerciseSet(
            pullUp,
            null,
            8,
            SetState.NotStated,
            bodyWeight = bodyWeight,
        )

        assert(set.oneRm == null)
    }

    @Test
    fun `assisted 1RM is missing without body weight`() {
        val set = ExerciseSet(pullUp, Weight.kilograms(25), 8, SetState.Completed())

        assert(set.oneRm == null)
    }

    @Test
    fun `assistance at or above body weight has no 1RM`() {
        val equal = ExerciseSet(
            pullUp,
            Weight.kilograms(80),
            8,
            SetState.Completed(),
            bodyWeight = bodyWeight,
        )
        val over = equal.copy(weight = Weight.kilograms(85))

        assert(equal.oneRm == null)
        assert(over.oneRm == null)
    }

    @Test
    fun `barbell 1RM ignores body weight`() {
        val set = ExerciseSet(
            catalogExercise("bench-press"),
            Weight.kilograms(60),
            8,
            SetState.Completed(),
            bodyWeight = bodyWeight,
        )

        assert(set.oneRm == Weight.kilograms(estimateOneRmKg(60f, 8)))
    }

    @Test
    fun `lower assistance is a higher adjusted 1RM`() {
        val harder = ExerciseSet(
            pullUp,
            Weight.kilograms(20),
            8,
            SetState.Completed(),
            bodyWeight = bodyWeight,
        )
        val easier = harder.copy(weight = Weight.kilograms(25))

        assert(harder.oneRm!! > easier.oneRm!!)
    }
}
