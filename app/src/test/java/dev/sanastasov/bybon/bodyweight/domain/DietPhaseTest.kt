package dev.sanastasov.bybon.bodyweight.domain

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.percentOf
import java.time.LocalDate
import org.junit.Test

class DietPhaseTest {

    private val start = LocalDate.of(2026, 9, 7)
    private val startWeight = BodyWeight.parseFromString("65.0")

    @Test
    fun `rejects gain target at or below start`() {
        val result = DietPhase.create(
            DietPhaseKind.Gain,
            start,
            startWeight,
            BodyWeight.parseFromString("64.0"),
            8,
        )
        assert(result is DietPhaseValidation.Invalid)
        assert(
            (result as DietPhaseValidation.Invalid).message ==
                "Gain target must be above current weight",
        )
    }

    @Test
    fun `rejects lose target at or above start`() {
        val result = DietPhase.create(
            DietPhaseKind.Lose,
            start,
            startWeight,
            BodyWeight.parseFromString("66.0"),
            8,
        )
        assert(result is DietPhaseValidation.Invalid)
        assert(
            (result as DietPhaseValidation.Invalid).message ==
                "Lose target must be below current weight",
        )
    }

    @Test
    fun `rejects gain faster than 0_5 percent per week`() {
        val result = DietPhase.create(
            DietPhaseKind.Gain,
            start,
            startWeight,
            BodyWeight.parseFromString("68.0"),
            4,
        )
        assert(result is DietPhaseValidation.Invalid)
    }

    @Test
    fun `accepts gain at 0_5 percent per week`() {
        val cap = startWeight.percentOf(MAX_GAIN_PERCENT_PER_WEEK)
        val target = BodyWeight(startWeight.value + cap.value * 8)
        val result = DietPhase.create(DietPhaseKind.Gain, start, startWeight, target, 8)
        assert(result is DietPhaseValidation.Valid)
        assert((result as DietPhaseValidation.Valid).phase is DietPhase.Gain)
    }

    @Test
    fun `rejects lose faster than 1 percent per week`() {
        val result = DietPhase.create(
            DietPhaseKind.Lose,
            start,
            startWeight,
            BodyWeight.parseFromString("55.0"),
            4,
        )
        assert(result is DietPhaseValidation.Invalid)
    }

    @Test
    fun `rejects weeks outside 1 to 30`() {
        val target = BodyWeight.parseFromString("66.0")
        assert(
            DietPhase.create(DietPhaseKind.Gain, start, startWeight, target, 0)
                is DietPhaseValidation.Invalid,
        )
        assert(
            DietPhase.create(DietPhaseKind.Gain, start, startWeight, target, 31)
                is DietPhaseValidation.Invalid,
        )
    }

    @Test
    fun `gain constructor rejects a target at or below start`() {
        try {
            DietPhase.Gain(start, startWeight, startWeight, 8)
            error("expected constructor to fail")
        } catch (error: IllegalArgumentException) {
            assert(error.message?.contains("above start weight") == true)
        }
    }

    @Test
    fun `rejects missing target weight`() {
        val result = DietPhase.create(DietPhaseKind.Maintain, start, startWeight, null)
        assert(result is DietPhaseValidation.Invalid)
        assert((result as DietPhaseValidation.Invalid).message == "Enter a target weight")
    }
}
