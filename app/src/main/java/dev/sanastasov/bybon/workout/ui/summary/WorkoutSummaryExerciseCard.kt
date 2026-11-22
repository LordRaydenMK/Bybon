package dev.sanastasov.bybon.workout.ui.summary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.sanastasov.bybon.ui.icons.Clock
import dev.sanastasov.bybon.workout.domain.OneRmChange
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.oneRmChange
import dev.sanastasov.bybon.workout.ui.MISSING_LOAD_LABEL
import dev.sanastasov.bybon.workout.ui.oneRmChangeColor

@Composable
internal fun WorkoutSummaryExerciseCard(
    exercise: WorkoutSummaryExerciseUi,
    onAction: (WorkoutSummaryAction) -> Unit,
) {
    val workSets = exercise.workSets
    val warmups = exercise.sets.filter { it.isWarmup }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                exercise.name,
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Open exercise details",
                        onClick = {
                            onAction(WorkoutSummaryAction.OnOpenExerciseDetails(exercise.id))
                        },
                    ),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
            exercise.previous?.let { PreviousWorkoutRow(it.workoutLabel) }
            exercise.bodyWeightLabel?.let { label ->
                val text = "Body weight $label"
                Text(
                    text,
                    Modifier.clearAndSetSemantics { contentDescription = text },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            exercise.notes.forEach { note ->
                Text(
                    note,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (warmups.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SummarySection("Warm-up sets") {
                warmups.forEach { set ->
                    WarmupSummaryRow(set)
                }
            }
        }
        if (workSets.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SummarySection("Working sets") {
                OneRmHighlights(exercise)
                WorkSetTable(workSets, exercise.showEffectiveLoad)
                WorkSetTotals(exercise)
            }
        }
    }
}

@Composable
private fun PreviousWorkoutRow(label: String) {
    Row(
        Modifier.semantics(mergeDescendants = true) { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Clock,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SummarySection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            title.uppercase(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}

@Composable
private fun WarmupSummaryRow(set: WorkoutSummarySetUi) {
    val load = formatSummaryLoad(set.weightKg, set.reps)
    Row(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Warmup set, $load"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(load, style = MaterialTheme.typography.bodyLarge)
        Surface(
            shape = RoundedCornerShape(50),
            color = Color.Transparent,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Text(
                "Warm-up",
                Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun OneRmHighlights(exercise: WorkoutSummaryExerciseUi) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OneRmHighlight(
            title = "Top-set e1RM",
            oneRm = exercise.topSetOneRm,
            previousOneRm = exercise.previous?.topSetOneRm,
            modifier = Modifier.weight(1f),
        )
        OneRmHighlight(
            title = "Average e1RM",
            oneRm = exercise.averageOneRm,
            previousOneRm = exercise.previous?.averageOneRm,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun OneRmHighlight(
    title: String,
    oneRm: Weight?,
    previousOneRm: Weight?,
    modifier: Modifier = Modifier,
) {
    val value = oneRm?.let { "${it.kilograms} kg" } ?: MISSING_LOAD_LABEL
    val change = oneRm?.let { oneRmChange(it, previousOneRm) }
    val vsPrevious = change?.vsPreviousLabel()
    Column(
        modifier.clearAndSetSemantics {
            contentDescription = listOfNotNull(title, value, vsPrevious).joinToString(", ")
        },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            value,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
        )
        if (vsPrevious != null) {
            Text(
                vsPrevious,
                color = change.oneRmChangeColor(),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun WorkSetTotals(exercise: WorkoutSummaryExerciseUi) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TotalRow("Total reps", "${exercise.totalReps}")
        TotalRow("Volume load", exercise.volumeLoadLabel)
    }
}

@Composable
private fun TotalRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "$label $value" },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            value,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
            textAlign = TextAlign.End,
        )
    }
}

internal fun formatSummaryLoad(weightKg: String?, reps: Int): String {
    val load = weightKg?.let { "$it kg" } ?: MISSING_LOAD_LABEL
    return "$load × $reps"
}

internal fun formatOneRmVsPrevious(current: Weight, previous: Weight): String? =
    oneRmChange(current, previous)?.vsPreviousLabel()

private fun OneRmChange.vsPreviousLabel(): String? = when (this) {
    is OneRmChange.Improved -> "↑ ${delta.kilograms} kg vs previous"
    is OneRmChange.Decreased -> "↓ ${delta.kilograms} kg vs previous"
    is OneRmChange.LessAssistance -> "↑ ${delta.kilograms} less vs previous"
    is OneRmChange.MoreAssistance -> "↓ ${delta.kilograms} more vs previous"
    OneRmChange.MoreReps -> "↑ reps vs previous"
    OneRmChange.FewerReps -> "↓ reps vs previous"
    OneRmChange.Unchanged -> null
}
