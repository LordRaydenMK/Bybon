package dev.sanastasov.bybon.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
internal val OneRmColumnMinWidth = 72.dp
internal val OneRmDeltaColumnMinWidth = 56.dp

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
        if (!expandLeading) {
            Spacer(Modifier.weight(1f))
        }
        HeaderCell(
            "1RM",
            oneRmModifier(),
            textAlign = TextAlign.End,
        )
        HeaderCell(
            "",
            oneRmDeltaModifier(),
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
    previousOneRm: Weight? = null,
) {
    val load = formatLoadedSet(weightKg, reps)
    val oneRmLabel = oneRm.toContentLoadLabel()
    val oneRmDelta = formatOneRmDelta(oneRm, previousOneRm)
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
        if (!expandLeading) {
            Spacer(Modifier.weight(1f))
        }
        Text(
            oneRmLabel,
            oneRmModifier(),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
        )
        Text(
            oneRmDelta,
            oneRmDeltaModifier(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
        )
    }
}

private fun RowScope.leadingModifier(expandLeading: Boolean): Modifier =
    if (expandLeading) Modifier.weight(1f) else Modifier.widthIn(min = CompactLeadingMinWidth)

private fun loadModifier(expandLeading: Boolean): Modifier =
    if (expandLeading) Modifier.widthIn(min = LoadColumnMinWidth) else Modifier

private fun oneRmModifier(): Modifier = Modifier.widthIn(min = OneRmColumnMinWidth)

private fun oneRmDeltaModifier(): Modifier = Modifier.widthIn(min = OneRmDeltaColumnMinWidth)

fun setLoadOneRmDescription(
    weightKg: String,
    reps: Int,
    oneRm: Weight?,
    previousOneRm: Weight? = null,
): String {
    val load = formatLoadedSet(weightKg, reps)
    val rm = oneRm?.let { "${it.kilograms} kg 1RM" } ?: MISSING_LOAD_LABEL
    return "$load, $rm, ${formatOneRmDelta(oneRm, previousOneRm)}"
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
