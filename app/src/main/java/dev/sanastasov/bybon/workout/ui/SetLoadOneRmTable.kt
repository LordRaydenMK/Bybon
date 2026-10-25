package dev.sanastasov.bybon.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
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
import dev.sanastasov.bybon.workout.domain.Weight

private val LoadColumnMinWidth = 96.dp
private val OneRmColumnMinWidth = 48.dp

@Composable
fun SetLoadOneRmHeader(leadingLabel: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HeaderCell(
            leadingLabel,
            Modifier.weight(1f),
            textAlign = TextAlign.Start,
        )
        HeaderCell(
            "kg x reps",
            Modifier.widthIn(min = LoadColumnMinWidth),
            textAlign = TextAlign.End,
        )
        HeaderCell(
            "1RM",
            Modifier.widthIn(min = OneRmColumnMinWidth),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
fun SetLoadOneRmRow(
    leading: String,
    weightKg: String,
    reps: Int,
    oneRm: Weight?,
    rowDescription: String,
    modifier: Modifier = Modifier,
) {
    val load = formatLoadedSet(weightKg, reps)
    val oneRmLabel = oneRm.toOneRmColumnLabel()
    Row(
        modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = rowDescription },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            leading,
            Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            load,
            Modifier.widthIn(min = LoadColumnMinWidth),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
        )
        Text(
            oneRmLabel,
            Modifier.widthIn(min = OneRmColumnMinWidth),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
        )
    }
}

fun setLoadOneRmDescription(weightKg: String, reps: Int, oneRm: Weight?): String {
    val load = formatLoadedSet(weightKg, reps)
    val rm = oneRm?.let { "${it.kilograms} kg 1RM" } ?: MISSING_LOAD_LABEL
    return "$load, $rm"
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
