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

@Composable
fun WeeklyWeightTrendCard(points: List<WeeklyTrendPointUi>, modifier: Modifier = Modifier) {
    val slotCount = points.maxOf { it.weekIndex } + 1
    val usesFallback = points.any { it.isLastSevenDaysFallback }
    val caption = buildString {
        append("Last $slotCount weeks")
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
    val values = remember(points) { points.map { it.kilograms.toDouble() } }
    val yRange = remember(values) { yAxisRange(values) }
    val lines = remember(values, colors.line, colors.surface) {
        listOf(weeklyAverageLine(values, colors.line, colors.surface))
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
        labelStyle = labelStyle,
        popupTextStyle = MaterialTheme.typography.labelSmall.copy(
            color = colorScheme.inverseOnSurface,
        ),
        popupContainer = colorScheme.inverseSurface,
    )
}

private fun weeklyAverageLine(values: List<Double>, lineColor: Color, surfaceColor: Color): Line =
    Line(
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
            enabled = true,
            radius = 4.dp,
            color = SolidColor(surfaceColor),
            strokeWidth = 2.dp,
            strokeColor = SolidColor(lineColor),
        ),
    )

private fun weeklyTrendDescription(points: List<WeeklyTrendPointUi>): String =
    points.joinToString(prefix = "Weekly body weight averages. ") {
        val suffix = if (it.isLastSevenDaysFallback) " last 7 day average" else ""
        "CW ${it.weekLabel} ${it.kilograms} kg$suffix"
    }

private fun weeklyTrendPopupText(
    points: List<WeeklyTrendPointUi>,
    popup: PopupProperties.Popup,
): String {
    val point = points.getOrNull(popup.valueIndex)
    val fallback = if (point?.isLastSevenDaysFallback == true) " · 7d" else ""
    return "CW ${point?.weekLabel ?: ""} · ${popup.value.format(1)} kg$fallback"
}
