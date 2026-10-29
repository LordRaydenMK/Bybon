package dev.sanastasov.bybon.workout.ui.summary

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.sanastasov.bybon.workout.ui.details.PersonalRecordUi
import dev.sanastasov.bybon.workout.ui.toOneRmColumnLabel

@Composable
internal fun AllTimeOneRmRow(personalRecord: PersonalRecordUi) {
    val oneRmLabel = personalRecord.oneRm.toOneRmColumnLabel()
    Row(
        Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = personalRecord.contentDescription() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "All-time 1RM",
            Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            personalRecord.dateLabel,
            Modifier.widthIn(min = 96.dp),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
        )
        Text(
            oneRmLabel,
            Modifier.widthIn(min = 48.dp),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
        )
    }
}

internal fun PersonalRecordUi.contentDescription(): String =
    "All-time 1RM ${oneRm.kilograms} kg, $dateLabel"
