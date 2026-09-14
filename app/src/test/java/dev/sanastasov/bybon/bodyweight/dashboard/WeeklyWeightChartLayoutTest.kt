package dev.sanastasov.bybon.bodyweight.dashboard

import org.junit.Test

class WeeklyWeightChartLayoutTest {

    @Test
    fun `pads a flat series so the y axis is not a single line`() {
        assert(yAxisRange(listOf(65.0, 65.0)) == 64.0..66.0)
    }

    @Test
    fun `adds padding around the observed range`() {
        val range = yAxisRange(listOf(64.0, 66.0))
        assert(range.start < 64.0)
        assert(range.endInclusive > 66.0)
    }
}
