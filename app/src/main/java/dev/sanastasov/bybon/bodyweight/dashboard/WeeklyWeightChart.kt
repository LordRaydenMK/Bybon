package dev.sanastasov.bybon.bodyweight.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp

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
    val lineColor = MaterialTheme.colorScheme.primary
    val fillColor = lineColor.copy(alpha = 0.18f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
    val pointColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val textMeasurer = rememberTextMeasurer()
    val colors = ChartColors(
        line = lineColor,
        fill = fillColor,
        grid = gridColor,
        point = pointColor,
        surface = surfaceColor,
    )
    val description = points.joinToString(prefix = "Weekly body weight averages. ") {
        val suffix = if (it.isLastSevenDaysFallback) " last 7 day average" else ""
        "CW ${it.weekLabel} ${it.kilograms} kg$suffix"
    }

    Canvas(modifier.semantics { contentDescription = description }) {
        val layout = chartLayout(points, textMeasurer, labelStyle)
        drawGrid(layout, colors.grid, textMeasurer, labelStyle)
        drawSeries(points, layout, colors)
        drawXAxisLabels(points, layout, textMeasurer, labelStyle)
    }
}

private data class ChartLayout(
    val points: List<Offset>,
    val yLabels: List<Pair<String, Float>>,
    val plotLeft: Float,
    val plotRight: Float,
    val plotTop: Float,
    val plotBottom: Float,
    val slotCount: Int,
)

private fun DrawScope.chartLayout(
    points: List<WeeklyTrendPointUi>,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
): ChartLayout {
    val kilograms = points.map { it.kilograms }
    val yRange = yAxisRange(kilograms)
    val yLabels = yAxisLabels(yRange)
    val yLabelWidth = yLabels.maxOf { textMeasurer.measure(it, labelStyle).size.width.toFloat() }
    val plotLeft = yLabelWidth + 8.dp.toPx()
    val plotRight = size.width - 4.dp.toPx()
    val plotTop = 8.dp.toPx()
    val plotBottom = size.height - 22.dp.toPx()
    val slotCount = maxOf(points.maxOf { it.weekIndex } + 1, 1)

    val plotted = points.map { point ->
        Offset(
            xForIndex(point.weekIndex, slotCount, plotLeft, plotRight),
            yForValue(point.kilograms, yRange, plotTop, plotBottom),
        )
    }
    val labeledGrid = yLabels.mapIndexed { index, label ->
        val y = plotTop + (plotBottom - plotTop) * index / (yLabels.lastIndex.coerceAtLeast(1))
        label to y
    }
    return ChartLayout(plotted, labeledGrid, plotLeft, plotRight, plotTop, plotBottom, slotCount)
}

private data class ChartColors(
    val line: Color,
    val fill: Color,
    val grid: Color,
    val point: Color,
    val surface: Color,
)

private fun DrawScope.drawGrid(
    layout: ChartLayout,
    gridColor: Color,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
) {
    layout.yLabels.forEach { (label, y) ->
        drawLine(
            color = gridColor,
            start = Offset(layout.plotLeft, y),
            end = Offset(layout.plotRight, y),
            strokeWidth = 1.dp.toPx(),
        )
        val measured = textMeasurer.measure(label, labelStyle)
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(
                layout.plotLeft - measured.size.width - 6.dp.toPx(),
                y - measured.size.height / 2f,
            ),
        )
    }
}

private fun DrawScope.drawSeries(
    points: List<WeeklyTrendPointUi>,
    layout: ChartLayout,
    colors: ChartColors,
) {
    if (layout.points.isEmpty()) return

    val fillPath = Path()
    layout.points.forEachIndexed { index, offset ->
        if (index == 0) {
            fillPath.moveTo(offset.x, layout.plotBottom)
            fillPath.lineTo(offset.x, offset.y)
        } else {
            fillPath.lineTo(offset.x, offset.y)
        }
    }
    fillPath.lineTo(layout.points.last().x, layout.plotBottom)
    fillPath.close()
    drawPath(
        fillPath,
        Brush.verticalGradient(
            colors = listOf(colors.fill, colors.fill.copy(alpha = 0f)),
            startY = layout.plotTop,
            endY = layout.plotBottom,
        ),
    )

    val fallbackIndex = points.indexOfFirst { it.isLastSevenDaysFallback }
    val solidEnd = if (fallbackIndex > 0) fallbackIndex else layout.points.size
    drawLinePath(layout.points.take(solidEnd), colors.line)
    if (fallbackIndex > 0) {
        drawLinePath(
            layout.points.slice(fallbackIndex - 1..fallbackIndex),
            colors.line.copy(alpha = 0.85f),
            PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx())),
        )
    }

    layout.points.forEachIndexed { index, offset ->
        val radius = if (index == layout.points.lastIndex) 5.dp.toPx() else 4.dp.toPx()
        if (points[index].isLastSevenDaysFallback) {
            drawCircle(color = colors.surface, radius = radius, center = offset)
            drawCircle(
                color = colors.point,
                radius = radius,
                center = offset,
                style = Stroke(width = 2.dp.toPx()),
            )
        } else {
            drawCircle(color = colors.point, radius = radius, center = offset)
            drawCircle(color = colors.surface, radius = radius / 2.5f, center = offset)
        }
    }
}

private fun DrawScope.drawLinePath(
    points: List<Offset>,
    color: Color,
    pathEffect: PathEffect? = null,
) {
    if (points.size < 2) return
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(
        path,
        color,
        style = Stroke(
            width = 2.5.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = pathEffect,
        ),
    )
}

private fun DrawScope.drawXAxisLabels(
    points: List<WeeklyTrendPointUi>,
    layout: ChartLayout,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
) {
    val lastIndex = points.lastIndex
    points.forEachIndexed { index, point ->
        if (
            !shouldDrawXLabel(
                point.weekIndex,
                layout.slotCount,
                index == lastIndex,
            )
        ) {
            return@forEachIndexed
        }
        val measured = textMeasurer.measure(point.weekLabel, labelStyle)
        val x = layout.points[index].x - measured.size.width / 2f
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(
                x.coerceIn(layout.plotLeft, layout.plotRight - measured.size.width),
                layout.plotBottom + 4.dp.toPx(),
            ),
        )
    }
}
