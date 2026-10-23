package dev.sanastasov.bybon.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

@Composable
fun SessionMeta(date: String, startTime: String, duration: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = "$date, started $startTime, duration $duration"
            },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            date,
            style = MaterialTheme.typography.titleMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
}
