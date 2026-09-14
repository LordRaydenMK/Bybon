package dev.sanastasov.bybon.bodyweight.dashboard

internal fun yAxisRange(values: List<Double>): ClosedFloatingPointRange<Double> {
    val lowest = values.min()
    val highest = values.max()
    if (lowest == highest) return (lowest - 1.0)..(highest + 1.0)
    val padding = maxOf((highest - lowest) * 0.2, 0.4)
    return (lowest - padding)..(highest + padding)
}
