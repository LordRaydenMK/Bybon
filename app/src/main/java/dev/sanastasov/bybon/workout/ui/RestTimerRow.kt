package dev.sanastasov.bybon.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sanastasov.bybon.ui.icons.Clock
import dev.sanastasov.bybon.workout.domain.RestTimer
import dev.sanastasov.bybon.workout.domain.formatRestClock
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlinx.coroutines.flow.map

@Composable
fun RestTimerRow(duration: Duration, modifier: Modifier = Modifier) {
    val formatted = duration.formatRestClock()
    Row(
        modifier.semantics(mergeDescendants = true) {
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

@Composable
fun RestGapRow(
    planned: Duration,
    modifier: Modifier = Modifier,
    activeRest: RestTimer? = null,
    now: () -> LocalDateTime = LocalDateTime::now,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (activeRest != null) {
            RestGapLive(activeRest, planned, now)
        } else {
            RestTimerRow(planned)
        }
    }
}

@Composable
private fun RestGapLive(rest: RestTimer, planned: Duration, now: () -> LocalDateTime) {
    val progress by remember(rest, now) {
        ticksEverySecond()
            .map { rest.progressUi(now()) }
            .takeUntil { it == null }
    }.collectAsStateWithLifecycle(
        initialValue = rest.progressUi(now()),
        minActiveState = Lifecycle.State.RESUMED,
    )
    val snapshot = progress
    if (snapshot == null) {
        RestTimerRow(planned)
        return
    }
    RestTimerRow(snapshot.remaining)
    LinearProgressIndicator(
        progress = { snapshot.fraction },
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = snapshot.remaining.restRemainingContentDescription()
            },
    )
}

internal data class RestProgressUi(
    val fraction: Float,
    val remaining: Duration,
)

internal fun RestTimer.progressUi(now: LocalDateTime): RestProgressUi? {
    val fraction = progress(now) ?: return null
    return RestProgressUi(fraction, remaining(now))
}

internal fun Duration.restRemainingContentDescription(): String {
    val totalSeconds = inWholeSeconds.coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val minutePart = when (minutes) {
        0L -> null
        1L -> "1 minute"
        else -> "$minutes minutes"
    }
    val secondPart = when {
        minutes > 0L && seconds == 0L -> null
        seconds == 1L -> "1 second"
        else -> "$seconds seconds"
    }
    return "Rest remaining ${listOfNotNull(minutePart, secondPart).joinToString(" ")}"
}

@Composable
fun SetPreviousAndRestRow(previousLabel: String?, rest: Duration?, modifier: Modifier = Modifier) {
    if (previousLabel == null && rest == null) return
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (previousLabel != null) {
            Text(
                previousLabel,
                Modifier
                    .weight(1f)
                    .clearAndSetSemantics { },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (rest != null) {
            RestTimerRow(rest)
        }
    }
}
