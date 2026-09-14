package dev.sanastasov.bybon.bodyweight.dashboard

import org.junit.Test

class WeeklyWeightChartLayoutTest {

    @Test
    fun `pads a flat series so the y axis is not a single line`() {
        assert(yAxisRange(listOf(65f, 65f)) == 64f..66f)
    }

    @Test
    fun `adds padding around the observed range`() {
        val range = yAxisRange(listOf(64f, 66f))
        assert(range.start < 64f)
        assert(range.endInclusive > 66f)
    }

    @Test
    fun `always draws the last x label and thins earlier ones`() {
        assert(shouldDrawXLabel(15, 16, isLast = true))
        assert(shouldDrawXLabel(0, 16, isLast = false))
        assert(!shouldDrawXLabel(1, 16, isLast = false))
        assert(shouldDrawXLabel(3, 16, isLast = false))
    }

    @Test
    fun `places a single week in the middle of the plot`() {
        assert(xForIndex(0, 1, 0f, 100f) == 50f)
        assert(xForIndex(0, 3, 0f, 100f) == 0f)
        assert(xForIndex(2, 3, 0f, 100f) == 100f)
    }
}
