package dev.sanastasov.bybon.workout.ui

import dev.sanastasov.bybon.workout.domain.Equipment
import dev.sanastasov.bybon.workout.domain.ExerciseSet
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.catalogExercise
import dev.sanastasov.bybon.workout.domain.estimateOneRmKg
import dev.sanastasov.bybon.workout.domain.oneRmChange
import org.junit.Test

class SetLoadFormatTest {

    @Test
    fun `assisted load is signed kilograms`() {
        assert(Weight.kilograms(25).toLoadLabel(Equipment.AssistedBodyWeight) == "-25")
        assert(Weight.kilograms(25).toLoadLabel(Equipment.Barbell) == "25")
    }

    @Test
    fun `blank assisted load is zero kilograms`() {
        val pullUp = ExerciseSet(
            catalogExercise("assisted-pull-ups"),
            null,
            8,
            SetState.Completed(),
        )
        val bench = ExerciseSet(
            catalogExercise("bench-press"),
            null,
            8,
            SetState.Completed(),
        )
        assert(pullUp.loggedLoadLabel() == "0")
        assert(pullUp.contentLoadLabel() == "0 kg")
        assert(bench.loggedLoadLabel() == "-")
        assert(bench.contentLoadLabel() == "-")
        assert(formatLoadedSet(pullUp.loggedLoadLabel(), 8) == "0 kg x 8")
        assert(
            formatLoadedSet(Weight.kilograms(25), 8, Equipment.AssistedBodyWeight) ==
                "-25 kg x 8",
        )
        assert(
            Weight.kilograms(25).toContentLoadLabel(Equipment.AssistedBodyWeight) == "-25 kg",
        )
    }

    @Test
    fun `1RM delta is dash when current matches previous`() {
        val oneRm = Weight.kilograms(63f)
        assert(formatOneRmDelta(oneRm, oneRm) == "-")
    }

    @Test
    fun `1RM delta is signed kilograms when current is higher`() {
        assert(
            formatOneRmDelta(Weight.kilograms(10.1f), Weight.kilograms(9f)) == "+1.1kg",
        )
    }

    @Test
    fun `1RM delta is signed kilograms when current is lower`() {
        assert(formatOneRmDelta(Weight.kilograms(9f), Weight.kilograms(10f)) == "-1kg")
    }

    @Test
    fun `1RM delta is dash when either estimate is missing`() {
        assert(formatOneRmDelta(null, Weight.kilograms(10f)) == "-")
        assert(formatOneRmDelta(Weight.kilograms(10f), null) == "-")
        assert(formatOneRmDelta(null, null) == "-")
    }

    @Test
    fun `1RM change uses an arrow and names no change`() {
        assert(
            oneRmChange(Weight.kilograms(26.25f), Weight.kilograms(25.67f))?.toLabel() ==
                "↑ 0.58 kg",
        )
        assert(
            oneRmChange(Weight.kilograms(24f), Weight.kilograms(25f))?.toLabel() ==
                "↓ 1 kg",
        )
        assert(oneRmChange(Weight.kilograms(25f), Weight.kilograms(25f))?.toLabel() == "No change")
    }

    @Test
    fun `1RM change rounds estimated top sets the same way as the summary`() {
        val current = Weight.kilograms(estimateOneRmKg(20f, 14))
        val previous = Weight.kilograms(estimateOneRmKg(20f, 13))
        assert(oneRmChange(current, previous)?.toLabel() == "↑ 0.66 kg")
    }
}
