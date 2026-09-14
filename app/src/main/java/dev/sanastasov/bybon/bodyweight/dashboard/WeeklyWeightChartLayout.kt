package dev.sanastasov.bybon.bodyweight.dashboard

import java.util.Locale

internal fun yAxisRange(values: List<Float>): ClosedFloatingPointRange<Float> {
    val lowest = values.min()
    val highest = values.max()
    if (lowest == highest) return (lowest - 1f)..(highest + 1f)
    val padding = maxOf((highest - lowest) * 0.2f, 0.4f)
    return (lowest - padding)..(highest + padding)
}

internal fun shouldDrawXLabel(weekIndex: Int, slotCount: Int, isLast: Boolean): Boolean {
    if (isLast) return true
    val step = when {
        slotCount <= 6 -> 1
        slotCount <= 12 -> 2
        else -> 3
    }
    return weekIndex % step == 0
}

internal fun yAxisLabels(range: ClosedFloatingPointRange<Float>): List<String> =
    (0..3).map { index ->
        val value = range.endInclusive - (range.endInclusive - range.start) * index / 3f
        String.format(Locale.US, "%.1f", value)
    }

internal fun xForIndex(index: Int, slotCount: Int, start: Float, end: Float): Float =
    if (slotCount <= 1) {
        (start + end) / 2f
    } else {
        start + (end - start) * index / (slotCount - 1)
    }

internal fun yForValue(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    top: Float,
    bottom: Float,
): Float {
    val fraction = (value - range.start) / (range.endInclusive - range.start)
    return bottom - fraction * (bottom - top)
}
