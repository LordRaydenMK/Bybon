package dev.sanastasov.bybon.workout.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import dev.sanastasov.bybon.workout.ui.formatLoadedSet
import dev.sanastasov.bybon.workout.ui.formatOneRmDelta
import dev.sanastasov.bybon.workout.ui.toContentLoadLabel

internal val OneRmColumnMinWidth = 72.dp
internal val OneRmDeltaColumnMinWidth = 56.dp
private val CompactLeadingMinWidth = 24.dp

@Composable
internal fun SetLoadOneRmHeader(modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HeaderCell("", Modifier.widthIn(min = CompactLeadingMinWidth), TextAlign.Start)
        HeaderCell("kg x reps", Modifier, TextAlign.Start)
        Spacer(Modifier.weight(1f))
        HeaderCell("1RM", Modifier.widthIn(min = OneRmColumnMinWidth), TextAlign.End)
        HeaderCell("vs prev", Modifier.widthIn(min = OneRmDeltaColumnMinWidth), TextAlign.End)
    }
}

@Composable
internal fun SetLoadOneRmRow(
    leading: String,
    weightKg: String,
    reps: Int,
    oneRm: Weight?,
    rowDescription: String,
    previousOneRm: Weight?,
    modifier: Modifier = Modifier,
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
            Modifier.widthIn(min = CompactLeadingMinWidth),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            load,
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
        )
        Spacer(Modifier.weight(1f))
        Text(
            oneRmLabel,
            Modifier.widthIn(min = OneRmColumnMinWidth),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
        )
        Text(
            oneRmDelta,
            Modifier.widthIn(min = OneRmDeltaColumnMinWidth),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
        )
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
