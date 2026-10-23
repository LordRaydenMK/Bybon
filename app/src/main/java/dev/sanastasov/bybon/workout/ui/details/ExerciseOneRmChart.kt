package dev.sanastasov.bybon.workout.ui.details

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.sanastasov.bybon.workout.domain.Weight
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
internal fun ExerciseOneRmTrendCard(
    points: List<ExerciseOneRmPointUi>,
    personalRecordOneRm: Weight?,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Text(
            "Estimated 1RM",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (personalRecordOneRm == null) {
                "Last 3 months · top-set 1RM"
            } else {
                "Last 3 months · top-set 1RM · dashed line is PR"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        ExerciseOneRmChart(
            points,
            personalRecordOneRm,
            Modifier
                .fillMaxWidth()
                .height(220.dp),
        )
    }
}

@Composable
private fun ExerciseOneRmChart(
    points: List<ExerciseOneRmPointUi>,
    personalRecordOneRm: Weight?,
    modifier: Modifier = Modifier,
) {
    val colors = oneRmChartColors()
    val values = remember(points) { points.map { it.kilograms.toDouble() } }
    val personalRecordKg = personalRecordOneRm?.kilogramsValue?.toDouble()
    val yRange = remember(values, personalRecordKg) {
        oneRmYAxisRange(values, personalRecordKg)
    }
    val lines = remember(values, personalRecordKg, colors) {
        buildList {
            add(oneRmTrendLine(values, colors.line, colors.surface))
            if (personalRecordKg != null) {
                add(personalRecordGuideline(values.size, personalRecordKg, colors.guideline))
            }
        }
    }
    LineChart(
        modifier = modifier.semantics { contentDescription = oneRmTrendDescription(points) },
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
            textStyle = colors.labelStyle.copy(textAlign = TextAlign.Center),
            padding = 12.dp,
            labels = chartDateLabels(points),
            rotation = LabelProperties.Rotation(
                mode = LabelProperties.Rotation.Mode.Force,
                degree = 0f,
            ),
        ),
        popupProperties = PopupProperties(
            textStyle = colors.popupTextStyle,
            containerColor = colors.popupContainer,
            mode = PopupProperties.Mode.PointMode(),
            contentBuilder = { popup -> oneRmTrendPopupText(points, popup.valueIndex) },
        ),
    )
}

private data class OneRmChartColors(
    val line: Color,
    val guideline: Color,
    val surface: Color,
    val outline: Color,
    val labelStyle: TextStyle,
    val popupTextStyle: TextStyle,
    val popupContainer: Color,
)

@Composable
private fun oneRmChartColors(): OneRmChartColors {
    val colorScheme = MaterialTheme.colorScheme
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = colorScheme.onSurfaceVariant)
    return OneRmChartColors(
        line = colorScheme.primary,
        guideline = colorScheme.tertiary,
        surface = colorScheme.surface,
        outline = colorScheme.outlineVariant,
        labelStyle = labelStyle,
        popupTextStyle = MaterialTheme.typography.labelSmall.copy(
            color = colorScheme.inverseOnSurface,
        ),
        popupContainer = colorScheme.inverseSurface,
    )
}

private fun oneRmTrendLine(values: List<Double>, lineColor: Color, surfaceColor: Color): Line =
    Line(
        label = "Top-set 1RM",
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

private fun personalRecordGuideline(pointCount: Int, kilograms: Double, color: Color): Line = Line(
    label = "Personal record",
    values = List(pointCount) { kilograms },
    color = SolidColor(color),
    curvedEdges = false,
    drawStyle = DrawStyle.Stroke(
        width = 1.5.dp,
        strokeStyle = StrokeStyle.Dashed(),
    ),
    strokeAnimationSpec = tween(700),
    gradientAnimationSpec = tween(0),
    gradientAnimationDelay = 0,
    dotProperties = DotProperties(enabled = false),
    popupProperties = PopupProperties(enabled = false),
)

internal fun oneRmYAxisRange(
    values: List<Double>,
    personalRecordKg: Double? = null,
): ClosedFloatingPointRange<Double> {
    val series = if (personalRecordKg == null) values else values + personalRecordKg
    val lowest = series.min()
    val highest = series.max()
    if (lowest == highest) return (lowest - 1.0)..(highest + 1.0)
    val padding = maxOf((highest - lowest) * 0.2, 0.4)
    return (lowest - padding)..(highest + padding)
}

internal fun chartDateLabels(points: List<ExerciseOneRmPointUi>): List<String> {
    val labels = points.map { it.dateLabel.replace(' ', '\n') }
    if (labels.size <= 8) return labels
    val step = ((labels.size - 1) / 4).coerceAtLeast(1)
    return labels.mapIndexed { index, label ->
        if (index == 0 || index == labels.lastIndex || index % step == 0) label else ""
    }
}

private fun oneRmTrendDescription(points: List<ExerciseOneRmPointUi>): String =
    points.joinToString(prefix = "Estimated 1RM of the top set. ") {
        "${it.dateLabel} ${it.weightKg} kg x ${it.reps}, ${it.oneRm.kilograms} kg"
    }
