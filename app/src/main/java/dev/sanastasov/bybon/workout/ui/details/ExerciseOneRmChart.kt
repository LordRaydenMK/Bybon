package dev.sanastasov.bybon.workout.ui.details

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.sanastasov.bybon.ui.LocalSkipChartAnimation
import dev.sanastasov.bybon.workout.domain.ExerciseHistoryWindow
import dev.sanastasov.bybon.workout.domain.Weight
import ir.ehsannarmani.compose_charts.models.DotProperties
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.Line
import ir.ehsannarmani.compose_charts.models.PopupProperties
import ir.ehsannarmani.compose_charts.models.StrokeStyle

@Composable
internal fun ExerciseOneRmTrendCard(
    points: List<ExerciseOneRmPointUi>,
    personalRecordOneRm: Weight?,
    highlightedTopSet: EstimatedOneRmHighlight? = null,
    historyWindow: ExerciseHistoryWindow = ExerciseHistoryWindow.LastThreeMonths,
    modifier: Modifier = Modifier,
) {
    val highlight = highlightedTopSet?.label()
    Column(modifier.fillMaxWidth()) {
        if (highlight == null) {
            Text(
                "Estimated 1RM",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics {
                        contentDescription = "Estimated 1RM, $highlight"
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Estimated 1RM",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    highlight,
                    Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            oneRmTrendCaption(points, personalRecordOneRm != null, historyWindow),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        ExerciseOneRmChart(
            points,
            personalRecordOneRm,
            Modifier
                .fillMaxWidth()
                .height(248.dp),
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
    val topSetValues = remember(points) { points.map { it.kilograms.toDouble() } }
    val averageValues = remember(points) { completeAverageValues(points) }
    val personalRecordKg = personalRecordOneRm?.kilogramsValue?.toDouble()
    val yRange = remember(topSetValues, averageValues, personalRecordKg) {
        oneRmYAxisRange(topSetValues + (averageValues ?: emptyList()), personalRecordKg)
    }
    val skipAnimation = LocalSkipChartAnimation.current
    val popup = remember(points, colors) {
        PopupProperties(
            textStyle = colors.popupTextStyle,
            containerColor = colors.popupContainer,
            mode = PopupProperties.Mode.PointMode(),
            contentBuilder = { item -> oneRmTrendPopupText(points, item.valueIndex) },
        )
    }
    val lines = remember(
        topSetValues,
        averageValues,
        personalRecordKg,
        colors,
        popup,
        skipAnimation,
    ) {
        oneRmChartLines(
            topSetValues,
            averageValues,
            personalRecordKg,
            colors,
            popup,
            skipAnimation,
        )
    }
    OneRmLineChart(
        points = points,
        lines = lines,
        yRange = yRange,
        colors = colors,
        popup = popup,
        skipAnimation = skipAnimation,
        modifier = modifier,
    )
}

internal data class OneRmChartColors(
    val line: Color,
    val averageLine: Color,
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
        averageLine = colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
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

internal fun oneRmTrendCaption(
    points: List<ExerciseOneRmPointUi>,
    hasPersonalRecord: Boolean,
    historyWindow: ExerciseHistoryWindow = ExerciseHistoryWindow.LastThreeMonths,
): String {
    val hasAverage = points.any { it.averageOneRm != null }
    return buildList {
        add(historyWindow.label())
        add("top-set 1RM")
        if (hasAverage) add("work-set average")
        if (hasPersonalRecord) add("dashed line is PR")
    }.joinToString(" · ")
}

internal fun completeAverageValues(points: List<ExerciseOneRmPointUi>): List<Double>? =
    points.map { it.averageOneRm?.kilogramsValue?.toDouble() }
        .takeIf { values -> values.isNotEmpty() && values.all { it != null } }
        ?.filterNotNull()

private fun oneRmChartLines(
    topSetValues: List<Double>,
    averageValues: List<Double>?,
    personalRecordKg: Double?,
    colors: OneRmChartColors,
    popupProperties: PopupProperties,
    skipAnimation: Boolean,
): List<Line> = buildList {
    add(
        oneRmTrendLine(
            label = "Top-set 1RM",
            values = topSetValues,
            lineColor = colors.line,
            surfaceColor = colors.surface,
            popupProperties = popupProperties,
            skipAnimation = skipAnimation,
        ),
    )
    if (averageValues != null) {
        add(
            secondaryTrendLine(
                label = "Average 1RM",
                values = averageValues,
                color = colors.averageLine,
                dashed = false,
                skipAnimation = skipAnimation,
            ),
        )
    }
    if (personalRecordKg != null) {
        add(
            secondaryTrendLine(
                label = "Personal record",
                values = List(topSetValues.size) { personalRecordKg },
                color = colors.guideline,
                dashed = true,
                skipAnimation = skipAnimation,
            ),
        )
    }
}

private fun oneRmTrendLine(
    label: String,
    values: List<Double>,
    lineColor: Color,
    surfaceColor: Color,
    popupProperties: PopupProperties,
    skipAnimation: Boolean,
): Line = Line(
    label = label,
    values = values,
    color = SolidColor(lineColor),
    firstGradientFillColor = lineColor.copy(alpha = 0.28f),
    secondGradientFillColor = Color.Transparent,
    curvedEdges = false,
    drawStyle = DrawStyle.Stroke(width = 2.5.dp),
    strokeAnimationSpec = tween(if (skipAnimation) 0 else 700),
    gradientAnimationSpec = tween(if (skipAnimation) 0 else 700),
    gradientAnimationDelay = if (skipAnimation) 0 else 150,
    dotProperties = DotProperties(
        enabled = true,
        radius = 4.dp,
        color = SolidColor(surfaceColor),
        strokeWidth = 2.dp,
        strokeColor = SolidColor(lineColor),
    ),
    popupProperties = popupProperties,
)

private fun secondaryTrendLine(
    label: String,
    values: List<Double>,
    color: Color,
    dashed: Boolean,
    skipAnimation: Boolean,
): Line = Line(
    label = label,
    values = values,
    color = SolidColor(color),
    curvedEdges = false,
    drawStyle = DrawStyle.Stroke(
        width = if (dashed) 1.5.dp else 1.25.dp,
        strokeStyle = if (dashed) StrokeStyle.Dashed() else StrokeStyle.Normal,
    ),
    strokeAnimationSpec = tween(if (skipAnimation) 0 else 700),
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

internal fun oneRmTrendDescription(points: List<ExerciseOneRmPointUi>): String {
    val prefix = if (points.any { it.averageOneRm != null }) {
        "Estimated 1RM of the top set and work-set average. "
    } else {
        "Estimated 1RM of the top set. "
    }
    return points.joinToString(prefix = prefix) { point ->
        val top = "${point.dateLabel} ${point.weightKg} kg x ${point.reps}, ${point.oneRm.kilograms} kg"
        val average = point.averageOneRm ?: return@joinToString top
        "$top, average ${average.kilograms} kg"
    }
}
