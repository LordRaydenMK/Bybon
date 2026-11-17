package dev.sanastasov.bybon.workout.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.format.TextStyle
import java.util.Locale

private val DayCircleSize = 32.dp
private const val FUTURE_DAY_ALPHA = 0.45f
private val WeekdayLetters = listOf("M", "T", "W", "T", "F", "S", "S")

@Composable
internal fun DaysTrainedSection(daysTrained: DaysTrainedUi, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Days trained",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        daysTrained.weeks.forEach { week ->
            TrainingWeekRow(week)
        }
        DaysTrainedLegend()
    }
}

@Composable
private fun TrainingWeekRow(week: TrainingWeekUi) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            week.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth()) {
            WeekdayLetters.forEach { letter ->
                Text(
                    letter,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            week.days.forEach { day ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    DayCircle(day)
                }
            }
        }
    }
}

@Composable
private fun DayCircle(day: TrainingDayUi) {
    val colors = MaterialTheme.colorScheme
    val fill = if (day.trained) colors.primary else Color.Transparent
    val textColor = if (day.trained) colors.onPrimary else colors.onSurfaceVariant
    val borderColor = when {
        day.isToday -> colors.primary
        else -> colors.outline
    }
    val weekday = day.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US)
    val description = buildString {
        append("$weekday ${day.date.dayOfMonth}")
        append(if (day.trained) ", trained" else ", rest")
        if (day.isToday) append(", today")
    }
    Box(
        Modifier
            .size(DayCircleSize)
            .alpha(if (day.isFuture && !day.trained) FUTURE_DAY_ALPHA else 1f)
            .background(fill, CircleShape)
            .border(if (day.isToday) 2.dp else 1.dp, borderColor, CircleShape)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            color = textColor,
            fontWeight = if (day.trained || day.isToday) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun DaysTrainedLegend() {
    val colors = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendSwatch(fill = colors.primary, border = colors.primary)
        Text(
            "Workout",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
        LegendSwatch(fill = Color.Transparent, border = colors.outline)
        Text(
            "Rest / upcoming",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun LegendSwatch(fill: Color, border: Color) {
    Box(
        Modifier
            .size(12.dp)
            .background(fill, CircleShape)
            .border(1.dp, border, CircleShape),
    )
}
