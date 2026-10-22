package dev.sanastasov.bybon.workout.ui.details

import dev.sanastasov.bybon.workout.domain.Weight
import org.junit.Test

class ExerciseOneRmChartLayoutTest {

    @Test
    fun `pads a flat series so the y axis is not a single line`() {
        assert(oneRmYAxisRange(listOf(70.0, 70.0)) == 69.0..71.0)
    }

    @Test
    fun `keeps every label when the series is short`() {
        val points = listOf(
            point("1 Aug"),
            point("15 Aug"),
            point("1 Sep"),
        )
        assert(chartDateLabels(points) == listOf("1 Aug", "15 Aug", "1 Sep"))
    }

    @Test
    fun `thins labels when the series is long`() {
        val points = (1..9).map { point("$it Aug") }
        val labels = chartDateLabels(points)
        assert(labels.first() == "1 Aug")
        assert(labels.last() == "9 Aug")
        assert(labels.count { it.isNotEmpty() } < points.size)
    }

    private fun point(label: String) = ExerciseOneRmPointUi(label, 70f, Weight.kilograms(70f))
}
