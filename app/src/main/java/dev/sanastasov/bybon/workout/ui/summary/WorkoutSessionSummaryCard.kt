package dev.sanastasov.bybon.workout.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.ui.TrainedMuscleChips
import dev.sanastasov.bybon.workout.ui.formatOneRmKg
import kotlin.math.abs

private val ImprovedGreen = Color(0xFF1B7A3D)

@Composable
internal fun WorkoutSessionSummaryCard(
    summary: WorkoutSessionSummaryUi,
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Workout completed!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    summary.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatsGrid(summary)
            HorizontalDivider()
            VolumeSection(summary.volumeLoadLabel)
            summary.setPerformance?.let { performance ->
                HorizontalDivider()
                SetPerformanceSection(performance)
            }
            if (summary.exercisePerformance.isNotEmpty()) {
                HorizontalDivider()
                ExercisePerformanceSection(summary.exercisePerformance)
            }
            if (summary.musclesTrained.isNotEmpty()) {
                HorizontalDivider()
                MuscleGroupsSection(summary.musclesTrained)
            }
        }
    }
}

@Composable
private fun StatsGrid(summary: WorkoutSessionSummaryUi) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCell("Duration", summary.duration, Modifier.weight(1f))
            StatCell("Exercises", summary.exerciseCount.toString(), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCell("Working sets", summary.workingSetCount.toString(), Modifier.weight(1f))
            StatCell("Total reps", summary.totalReps.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun VolumeSection(volumeLoadLabel: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Training volume",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            volumeLoadLabel,
            style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Working sets only",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SetPerformanceSection(performance: SetPerformanceUi) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Set performance",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Row(Modifier.fillMaxWidth()) {
            SetPerformanceCell(
                count = performance.improved,
                label = "Improved",
                color = ImprovedGreen,
                modifier = Modifier.weight(1f),
            )
            SetPerformanceCell(
                count = performance.maintained,
                label = "Maintained",
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            SetPerformanceCell(
                count = performance.declined,
                label = "Declined",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SetPerformanceCell(
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            count.toString(),
            style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExercisePerformanceSection(rows: List<ExerciseAverageOneRmUi>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Exercise performance",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        rows.forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(row.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    row.changeLabel,
                    color = row.changeColor(),
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        Text(
            "Average e1RM change compared with previous exercise sessions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MuscleGroupsSection(muscles: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Muscle groups",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        TrainedMuscleChips(muscles)
    }
}

@Composable
private fun ExerciseAverageOneRmUi.changeColor(): Color = when {
    changeLabel.startsWith("↑") -> ImprovedGreen
    changeLabel.startsWith("↓") -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

internal fun WorkoutSessionSummaryUi.contentDescription(): String = buildString {
    append("Workout completed, $subtitle, ")
    append("duration $duration, $exerciseCount exercises, ")
    append("$workingSetCount working sets, $totalReps total reps, ")
    append("training volume $volumeLoadLabel")
    setPerformance?.let {
        append(", ${it.improved} improved, ${it.maintained} maintained, ${it.declined} declined")
    }
    if (exercisePerformance.isNotEmpty()) {
        append(
            ", exercise performance ${
                exercisePerformance.joinToString { "${it.name} ${it.changeLabel}" }
            }",
        )
    }
    if (musclesTrained.isNotEmpty()) {
        append(", muscle groups ${musclesTrained.joinToString()}")
    }
}

internal fun formatAverageOneRmChange(current: Weight, previous: Weight): String {
    val deltaHundredths = oneRmDeltaHundredths(current, previous)
    if (deltaHundredths == 0) return "No change"
    val amount = formatOneRmKg(abs(deltaHundredths) / 100f)
    val arrow = if (deltaHundredths > 0) "↑" else "↓"
    return "$arrow $amount kg"
}
