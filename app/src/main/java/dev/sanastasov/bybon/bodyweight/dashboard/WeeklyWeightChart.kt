package dev.sanastasov.bybon.bodyweight.dashboard

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.extensions.format
import ir.ehsannarmani.compose_charts.models.DividerProperties
import ir.ehsannarmani.compose_charts.models.DotProperties
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.IndicatorCount
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line
import ir.ehsannarmani.compose_charts.models.LineProperties
import ir.ehsannarmani.compose_charts.models.PopupProperties
import ir.ehsannarmani.compose_charts.models.StrokeStyle

@Composable
fun WeeklyWeightTrendCard(points: List<WeeklyTrendPointUi>, modifier: Modifier = Modifier) {
    val lastHistoricalIndex = points.filterNot { it.isFuture }.maxOfOrNull { it.weekIndex } ?: 0
    val futureCount = points.count { it.isFuture }
    val usesFallback = points.any { it.isLastSevenDaysFallback }
    val caption = buildString {
        append("Last ${lastHistoricalIndex + 1} weeks")
        if (futureCount > 0) append(" · $futureCount weeks ahead")
        if (usesFallback) append(" · current week uses last 7d average")
    }

    Card(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Text("Weekly average", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            WeeklyWeightChart(
                points,
                Modifier
                    .fillMaxWidth()
                    .height(196.dp),
            )
        }
    }
}

@Composable
private fun WeeklyWeightChart(points: List<WeeklyTrendPointUi>, modifier: Modifier = Modifier) {
    val colors = weeklyChartColors()
    val actualValues = remember(points) { actualLineValues(points) }
    val yRange = remember(points, actualValues) {
        yAxisRange(chartRangeValues(points, actualValues))
    }
    val hasFuture = points.any { it.isFuture }
    val lines = remember(points, actualValues, colors, hasFuture) {
        buildList {
            add(
                weeklyAverageLine(
                    actualValues,
                    colors.line,
                    colors.surface,
                    !hasFuture,
                ),
            )
            overlayLine(
                points.map { point -> point.projectedKilograms },
                "Projected",
                colors.overlay,
            )?.let(::add)
            overlayLine(
                points.map { point -> point.maintainHighKilograms },
                "High",
                colors.overlay,
            )?.let(::add)
            overlayLine(
                points.map { point -> point.maintainLowKilograms },
                "Low",
                colors.overlay,
            )?.let(::add)
        }
    }
    val description = weeklyTrendDescription(points)

    WeeklyAverageLineChart(
        points,
        yRange,
        lines,
        colors,
        modifier.semantics { contentDescription = description },
    )
}

@Composable
private fun WeeklyAverageLineChart(
    points: List<WeeklyTrendPointUi>,
    yRange: ClosedFloatingPointRange<Double>,
    lines: List<Line>,
    colors: WeeklyChartColors,
    modifier: Modifier = Modifier,
) {
    LineChart(
        modifier = modifier,
        data = lines,
        curvedEdges = false,
        animationDelay = 80,
        minValue = yRange.start,
        maxValue = yRange.endInclusive,
        labelHelperProperties = LabelHelperProperties(enabled = false),
        dividerProperties = DividerProperties(
            xAxisProperties = LineProperties(color = SolidColor(colors.outline)),
            yAxisProperties = LineProperties(color = SolidColor(colors.outline)),
        ),
        gridProperties = GridProperties(
            xAxisProperties = GridProperties.AxisProperties(
                color = SolidColor(colors.outline.copy(alpha = 0.7f)),
                lineCount = 4,
            ),
            yAxisProperties = GridProperties.AxisProperties(enabled = false),
        ),
        indicatorProperties = HorizontalIndicatorProperties(
            textStyle = colors.labelStyle,
            count = IndicatorCount.CountBased(count = 4),
            padding = 8.dp,
            contentBuilder = { value -> value.format(1) },
        ),
        labelProperties = LabelProperties(
            enabled = true,
            textStyle = colors.labelStyle,
            padding = 8.dp,
            labels = points.map { it.weekLabel },
        ),
        popupProperties = PopupProperties(
            textStyle = colors.popupTextStyle,
            containerColor = colors.popupContainer,
            mode = PopupProperties.Mode.PointMode(),
            contentBuilder = { popup -> weeklyTrendPopupText(points, popup) },
        ),
    )
}

private data class WeeklyChartColors(
    val line: Color,
    val surface: Color,
    val outline: Color,
    val overlay: Color,
    val labelStyle: TextStyle,
    val popupTextStyle: TextStyle,
    val popupContainer: Color,
)

@Composable
private fun weeklyChartColors(): WeeklyChartColors {
    val colorScheme = MaterialTheme.colorScheme
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = colorScheme.onSurfaceVariant)
    return WeeklyChartColors(
        line = colorScheme.primary,
        surface = colorScheme.surface,
        outline = colorScheme.outlineVariant,
        overlay = colorScheme.tertiary,
        labelStyle = labelStyle,
        popupTextStyle = MaterialTheme.typography.labelSmall.copy(
            color = colorScheme.inverseOnSurface,
        ),
        popupContainer = colorScheme.inverseSurface,
    )
}

private fun weeklyAverageLine(
    values: List<Double>,
    lineColor: Color,
    surfaceColor: Color,
    dotsEnabled: Boolean,
): Line = Line(
    label = "Weekly average",
    values = values,
    color = SolidColor(lineColor),
    firstGradientFillColor = lineColor.copy(alpha = 0.28f),
    secondGradientFillColor = Color.Transparent,
    curvedEdges = false,
    drawStyle = DrawStyle.Stroke(width = 2.5.dp),
    strokeAnimationSpec = tween(700),
    gradientAnimationSpec = tween(700),
    gradientAnimationDelay = 150,
    dotProperties = DotProperties(
        enabled = dotsEnabled,
        radius = 4.dp,
        color = SolidColor(surfaceColor),
        strokeWidth = 2.dp,
        strokeColor = SolidColor(lineColor),
    ),
)

@Suppress("ReturnCount")
private fun overlayLine(values: List<Float?>, label: String, color: Color): Line? {
    if (values.all { it == null }) return null
    var last = values.firstOrNull { it != null } ?: return null
    val filled = values.map { value ->
        val next = value ?: last
        last = next
        next.toDouble()
    }
    return Line(
        label = label,
        values = filled,
        color = SolidColor(color),
        firstGradientFillColor = Color.Transparent,
        secondGradientFillColor = Color.Transparent,
        curvedEdges = false,
        drawStyle = DrawStyle.Stroke(
            width = 1.5.dp,
            strokeStyle = StrokeStyle.Dashed(intervals = floatArrayOf(10f, 10f)),
        ),
        strokeAnimationSpec = tween(700),
        gradientAnimationSpec = tween(700),
        dotProperties = DotProperties(enabled = false),
    )
}

private fun actualLineValues(points: List<WeeklyTrendPointUi>): List<Double> {
    var last = points.firstNotNullOfOrNull { it.kilograms } ?: 0f
    return points.map { point ->
        val next = point.kilograms ?: point.projectedKilograms ?: midpoint(point) ?: last
        last = next
        next.toDouble()
    }
}

private fun midpoint(point: WeeklyTrendPointUi): Float? {
    val low = point.maintainLowKilograms
    val high = point.maintainHighKilograms
    return if (low != null && high != null) (low + high) / 2f else null
}

private fun chartRangeValues(points: List<WeeklyTrendPointUi>, actual: List<Double>): List<Double> =
    actual + points.flatMap { point ->
        listOfNotNull(
            point.projectedKilograms?.toDouble(),
            point.maintainLowKilograms?.toDouble(),
            point.maintainHighKilograms?.toDouble(),
        )
    }

private fun weeklyTrendDescription(points: List<WeeklyTrendPointUi>): String =
    points.joinToString(prefix = "Weekly body weight averages. ") {
        val suffix = if (it.isLastSevenDaysFallback) " last 7 day average" else ""
        val kg = it.kilograms?.toString() ?: "projected"
        "CW ${it.weekLabel} $kg kg$suffix"
    }

private fun weeklyTrendPopupText(
    points: List<WeeklyTrendPointUi>,
    popup: PopupProperties.Popup,
): String {
    val point = points.getOrNull(popup.valueIndex)
    val fallback = if (point?.isLastSevenDaysFallback == true) " · 7d" else ""
    val future = if (point?.isFuture == true) " · projected" else ""
    return "CW ${point?.weekLabel ?: ""} · ${popup.value.format(1)} kg$fallback$future"
}
