package dev.sanastasov.bybon.workout.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val ImprovedGreen = Color(0xFF2E7D32)
private val DeclinedRed = Color(0xFFE57373)

@Composable
internal fun LastSevenDaysSummaryCard(
    summary: LastSevenDaysSummaryUi,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier
            .fillMaxWidth()
            .semantics { contentDescription = summary.contentDescription() },
        colors = CardDefaults.outlinedCardColors(),
        border = CardDefaults.outlinedCardBorder(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SummaryHeader(summary.dateRange)
            StatsGrid(summary)
            if (summary.performance != null) {
                HorizontalDivider()
                PerformanceSection(summary.performance)
            }
            if (summary.musclesTrained.isNotEmpty()) {
                HorizontalDivider()
                MusclesSection(summary.musclesTrained)
            }
        }
    }
}

@Composable
private fun SummaryHeader(dateRange: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Last 7 days",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            dateRange,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatsGrid(summary: LastSevenDaysSummaryUi) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCell(summary.workoutCount.toString(), "Workouts", Modifier.weight(1f))
            StatCell(summary.trainingTime, "Training time", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCell(summary.workingSetCount.toString(), "Working sets", Modifier.weight(1f))
            StatCell(summary.totalReps.toString(), "Total reps", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Bold,
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PerformanceSection(performance: LastSevenDaysPerformanceUi) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Performance",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "↑ ${performance.improved} improved",
                color = ImprovedGreen,
                fontWeight = FontWeight.Medium,
            )
            Text(
                "${performance.maintained} maintained · ${performance.declined} declined",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PerformanceBar(performance)
    }
}

@Composable
private fun PerformanceBar(performance: LastSevenDaysPerformanceUi) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
    ) {
        BarSegment(performance.improved, ImprovedGreen)
        BarSegment(performance.maintained, MaterialTheme.colorScheme.outlineVariant)
        BarSegment(performance.declined, DeclinedRed)
    }
}

@Composable
private fun RowScope.BarSegment(count: Int, color: Color) {
    if (count <= 0) return
    Box(
        Modifier
            .weight(count.toFloat())
            .fillMaxHeight()
            .background(color),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MusclesSection(muscles: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Muscles trained",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            muscles.forEach { muscle ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Text(
                        muscle,
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

internal fun LastSevenDaysSummaryUi.contentDescription(): String = buildString {
    append("Last 7 days, $dateRange, ")
    append("$workoutCount workouts, $trainingTime training time, ")
    append("$workingSetCount working sets, $totalReps total reps")
    performance?.let {
        append(", ${it.improved} improved, ${it.maintained} maintained, ${it.declined} declined")
    }
    if (musclesTrained.isNotEmpty()) {
        append(", muscles trained ${musclesTrained.joinToString()}")
    }
}
