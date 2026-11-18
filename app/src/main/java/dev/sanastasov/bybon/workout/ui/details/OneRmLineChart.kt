package dev.sanastasov.bybon.workout.ui.details

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.extensions.format
import ir.ehsannarmani.compose_charts.models.DividerProperties
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.IndicatorCount
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line
import ir.ehsannarmani.compose_charts.models.LineProperties
import ir.ehsannarmani.compose_charts.models.PopupProperties

@Composable
internal fun OneRmLineChart(
    points: List<ExerciseOneRmPointUi>,
    lines: List<Line>,
    yRange: ClosedFloatingPointRange<Double>,
    colors: OneRmChartColors,
    popup: PopupProperties,
    skipAnimation: Boolean,
    modifier: Modifier,
) {
    LineChart(
        modifier = modifier.semantics { contentDescription = oneRmTrendDescription(points) },
        data = lines,
        curvedEdges = false,
        animationDelay = if (skipAnimation) 0 else 80,
        minValue = yRange.start,
        maxValue = yRange.endInclusive,
        labelHelperProperties = LabelHelperProperties(
            enabled = true,
            textStyle = colors.labelStyle,
        ),
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
            textStyle = colors.labelStyle.copy(textAlign = TextAlign.Center),
            padding = 12.dp,
            labels = chartDateLabels(points),
            rotation = LabelProperties.Rotation(
                mode = LabelProperties.Rotation.Mode.Force,
                degree = 0f,
            ),
        ),
        popupProperties = popup,
    )
}
