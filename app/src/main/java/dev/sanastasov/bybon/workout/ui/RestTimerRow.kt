package dev.sanastasov.bybon.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.sanastasov.bybon.ui.icons.Clock
import dev.sanastasov.bybon.workout.domain.formatRestClock
import kotlin.time.Duration

@Composable
fun RestTimerRow(duration: Duration, modifier: Modifier = Modifier) {
    val formatted = duration.formatRestClock()
    Row(
        modifier
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Rest $formatted"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            Clock,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            formatted,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
