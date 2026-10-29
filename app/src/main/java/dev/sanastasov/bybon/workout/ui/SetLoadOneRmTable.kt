package dev.sanastasov.bybon.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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

private val CompactLeadingMinWidth = 24.dp
private val LoadColumnMinWidth = 96.dp
private val OneRmColumnMinWidth = 48.dp

@Composable
fun SetLoadOneRmHeader(
    leadingLabel: String,
    modifier: Modifier = Modifier,
    expandLeading: Boolean = true,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HeaderCell(
            leadingLabel,
            leadingModifier(expandLeading),
            textAlign = TextAlign.Start,
        )
        HeaderCell(
            "kg x reps",
            loadModifier(expandLeading),
            textAlign = if (expandLeading) TextAlign.End else TextAlign.Start,
        )
        HeaderCell(
            "1RM",
            oneRmModifier(expandLeading),
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
    expandLeading: Boolean = true,
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
            leadingModifier(expandLeading),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            load,
            loadModifier(expandLeading),
            textAlign = if (expandLeading) TextAlign.End else TextAlign.Start,
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
        )
        Text(
            oneRmLabel,
            oneRmModifier(expandLeading),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
        )
    }
}

private fun RowScope.leadingModifier(expandLeading: Boolean): Modifier =
    if (expandLeading) Modifier.weight(1f) else Modifier.widthIn(min = CompactLeadingMinWidth)

private fun loadModifier(expandLeading: Boolean): Modifier =
    if (expandLeading) Modifier.widthIn(min = LoadColumnMinWidth) else Modifier

private fun RowScope.oneRmModifier(expandLeading: Boolean): Modifier =
    if (expandLeading) Modifier.widthIn(min = OneRmColumnMinWidth) else Modifier.weight(1f)

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
