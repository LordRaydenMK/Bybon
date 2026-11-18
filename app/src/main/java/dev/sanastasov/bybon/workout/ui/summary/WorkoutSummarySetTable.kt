package dev.sanastasov.bybon.workout.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.sanastasov.bybon.workout.ui.MISSING_LOAD_LABEL
import dev.sanastasov.bybon.workout.ui.toOneRmColumnLabel

@Composable
internal fun WorkSetTable(workSets: List<WorkoutSummarySetUi>, showEffectiveLoad: Boolean) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WorkSetHeader(showEffectiveLoad)
        workSets.forEach { set ->
            WorkSetRow(set, showEffectiveLoad)
        }
    }
}

@Composable
private fun WorkSetHeader(showEffectiveLoad: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HeaderCell("Set", Modifier.weight(0.7f), TextAlign.Start)
        HeaderCell("Weight", Modifier.weight(1f), TextAlign.End)
        if (showEffectiveLoad) {
            HeaderCell("Effective", Modifier.weight(1.1f), TextAlign.End)
        }
        HeaderCell("Reps", Modifier.weight(0.8f), TextAlign.End)
        HeaderCell("e1RM", Modifier.weight(1f), TextAlign.End)
    }
}

@Composable
private fun WorkSetRow(set: WorkoutSummarySetUi, showEffectiveLoad: Boolean) {
    val number = checkNotNull(set.number)
    Row(
        Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = set.contentDescription(
                    showEffectiveLoad,
                )
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BodyCell("$number", Modifier.weight(0.7f), TextAlign.Start)
        BodyCell(set.weightKg ?: MISSING_LOAD_LABEL, Modifier.weight(1f), TextAlign.End)
        if (showEffectiveLoad) {
            BodyCell(set.effectiveLoadKg ?: MISSING_LOAD_LABEL, Modifier.weight(1f), TextAlign.End)
        }
        BodyCell("${set.reps}", Modifier.weight(0.8f), TextAlign.End)
        BodyCell(set.oneRm.toOneRmColumnLabel(), Modifier.weight(1f), TextAlign.End)
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier, textAlign: TextAlign) {
    Text(
        text,
        modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium,
        textAlign = textAlign,
    )
}

@Composable
private fun BodyCell(text: String, modifier: Modifier, textAlign: TextAlign) {
    Text(
        text,
        modifier,
        textAlign = textAlign,
        style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
        maxLines = 1,
        overflow = TextOverflow.Clip,
    )
}

internal fun WorkoutSummarySetUi.contentDescription(showEffectiveLoad: Boolean = false): String {
    val number = checkNotNull(number)
    val load = formatSummaryLoad(weightKg, reps)
    val effective = if (showEffectiveLoad) {
        val kg = effectiveLoadKg?.let { "$it kg" } ?: MISSING_LOAD_LABEL
        ", $kg effective"
    } else {
        ""
    }
    val rm = oneRm?.let { "${it.kilograms} kg e1RM" } ?: MISSING_LOAD_LABEL
    return "$number. $load$effective, $rm"
}
