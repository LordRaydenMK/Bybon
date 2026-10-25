package dev.sanastasov.bybon.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

@Composable
fun SessionMeta(date: String, startTime: String, duration: String, modifier: Modifier = Modifier) {
    FlowRow(
        modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = "$date, started $startTime, duration $duration"
            },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            date,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "Start $startTime",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            "Duration $duration",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
