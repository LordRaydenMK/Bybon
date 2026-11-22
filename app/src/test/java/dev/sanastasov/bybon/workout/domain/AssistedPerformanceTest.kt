package dev.sanastasov.bybon.workout.domain

import dev.sanastasov.bybon.workout.data.completedAssistedExercise
import org.junit.Test

class AssistedPerformanceTest {

    private val pullUp = catalogExercise("assisted-pull-ups")

    @Test
    fun `performance top set prefers less assistance when 1RM is missing`() {
        val exercise = completedAssistedExercise(
            "assisted-pull-ups",
            bodyWeightKg = null,
            25f to 10,
            15f to 8,
            null to 6,
        )

        assert(exercise.topSet.weight == Weight.kilograms(25))
        assert(exercise.performanceTopSet().weight == null)
        assert(exercise.performanceTopSet().reps == 6)
    }

    @Test
    fun `equal assistance prefers more reps`() {
        val exercise = completedAssistedExercise(
            "assisted-pull-ups",
            bodyWeightKg = null,
            15f to 8,
            15f to 10,
        )

        assert(exercise.performanceTopSet().reps == 10)
    }

    @Test
    fun `missing body weight compares assistance instead of 1RM`() {
        val previous = assisted(25f, 8, bodyWeight = null)
        val current = assisted(20f, 8, bodyWeight = 80f)

        assert(current.oneRm != null)
        assert(previous.oneRm == null)
        assert(
            current.performanceChange(previous) == OneRmChange.Improved(
                OneRmChange.How.Assistance(Weight.kilograms(5)),
            ),
        )
    }

    @Test
    fun `both body weights keep the estimated 1RM delta`() {
        val previous = assisted(25f, 8, bodyWeight = 80f)
        val current = assisted(20f, 8, bodyWeight = 80f)

        assert(
            current.performanceChange(previous) == oneRmChange(
                checkNotNull(current.oneRm),
                previous.oneRm,
            ),
        )
        assert(current.performanceChange(previous) is OneRmChange.Improved)
    }

    @Test
    fun `more assistance is a decline and equal assistance uses reps`() {
        val baseline = assisted(20f, 8, bodyWeight = null)

        val moreAssistance = assisted(25f, 8, bodyWeight = null)
        val moreReps = assisted(20f, 10, bodyWeight = null)
        val fewerReps = assisted(20f, 6, bodyWeight = null)
        val same = assisted(20f, 8, bodyWeight = null)
        val unassisted = assisted(null, 8, bodyWeight = null)
        val earlierUnassisted = assisted(null, 6, bodyWeight = null)

        assert(
            moreAssistance.performanceChange(baseline) == OneRmChange.Declined(
                OneRmChange.How.Assistance(Weight.kilograms(5)),
            ),
        )
        assert(moreReps.performanceChange(baseline) == OneRmChange.Improved(OneRmChange.How.Reps))
        assert(fewerReps.performanceChange(baseline) == OneRmChange.Declined(OneRmChange.How.Reps))
        assert(same.performanceChange(baseline) == OneRmChange.Maintained)
        assert(
            unassisted.performanceChange(baseline) == OneRmChange.Improved(
                OneRmChange.How.Assistance(Weight.kilograms(20)),
            ),
        )
        assert(
            baseline.performanceChange(earlierUnassisted) == OneRmChange.Declined(
                OneRmChange.How.Assistance(Weight.kilograms(20)),
            ),
        )
        val repChange = unassisted.performanceChange(earlierUnassisted)
        assert(repChange == OneRmChange.Improved(OneRmChange.How.Reps))
    }

    @Test
    fun `loaded sets still compare estimated 1RM and skip a missing load`() {
        val current = loaded(50f, 8)
        val previous = loaded(45f, 8)
        val missing = loaded(weight = null, reps = 8)

        assert(
            current.performanceChange(previous) == oneRmChange(
                checkNotNull(current.oneRm),
                previous.oneRm,
            ),
        )
        assert(missing.performanceChange(previous) == null)
    }

    private fun assisted(assist: Float?, reps: Int, bodyWeight: Float?): ExerciseSet = ExerciseSet(
        pullUp,
        assist?.let(Weight::kilograms),
        reps,
        SetState.Completed(),
        bodyWeight = bodyWeight?.let(Weight::kilograms),
    )

    private fun loaded(weight: Float?, reps: Int): ExerciseSet = ExerciseSet(
        catalogExercise("bench-press"),
        weight?.let(Weight::kilograms),
        reps,
        SetState.Completed(),
    )
}
