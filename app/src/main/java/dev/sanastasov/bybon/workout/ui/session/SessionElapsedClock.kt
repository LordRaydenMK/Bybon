package dev.sanastasov.bybon.workout.ui.session

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sanastasov.bybon.workout.domain.elapsedSince
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@Composable
internal fun SessionElapsedClock(
    startedAt: LocalDateTime,
    modifier: Modifier = Modifier,
    now: () -> LocalDateTime = LocalDateTime::now,
) {
    val elapsed by remember(startedAt, now) {
        ticksEverySecond().map { elapsedSince(startedAt, now()) }
    }.collectAsStateWithLifecycle(
        initialValue = elapsedSince(startedAt, now()),
        minActiveState = Lifecycle.State.RESUMED,
    )
    Text(
        elapsed.formatElapsedClock(),
        modifier
            .padding(end = 8.dp)
            .clearAndSetSemantics {
                contentDescription = elapsed.elapsedClockContentDescription()
            },
        color = MaterialTheme.colorScheme.onSurface,
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.titleMedium,
    )
}

private fun ticksEverySecond(): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        delay(1.seconds)
    }
}

internal fun Duration.formatElapsedClock(): String {
    val totalSeconds = inWholeSeconds.coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}

internal fun Duration.elapsedClockContentDescription(): String {
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
    return "Elapsed ${listOfNotNull(minutePart, secondPart).joinToString(" ")}"
}

@Preview
@Composable
private fun SessionElapsedClockPreview() {
    val startedAt = LocalDateTime.of(2026, 10, 23, 12, 0, 0)
    SessionElapsedClock(
        startedAt,
        now = { startedAt.plusMinutes(50).plusSeconds(20) },
    )
}
