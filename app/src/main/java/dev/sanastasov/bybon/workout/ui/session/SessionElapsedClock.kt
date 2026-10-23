package dev.sanastasov.bybon.workout.ui.session

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.sanastasov.bybon.workout.domain.elapsedClockContentDescription
import dev.sanastasov.bybon.workout.domain.elapsedSince
import dev.sanastasov.bybon.workout.domain.formatElapsedClock
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

@Composable
internal fun SessionElapsedClock(
    startedAt: LocalDateTime,
    modifier: Modifier = Modifier,
    now: () -> LocalDateTime = LocalDateTime::now,
) {
    val elapsed by produceState(startedAt.clockSnapshot(now()), startedAt) {
        while (true) {
            val snapshot = startedAt.clockSnapshot(now())
            value = snapshot
            delay(snapshot.delayUntilNextSecond)
        }
    }
    Text(
        elapsed.label,
        modifier
            .padding(end = 8.dp)
            .clearAndSetSemantics { contentDescription = elapsed.contentDescription },
        color = MaterialTheme.colorScheme.onSurface,
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.titleMedium,
    )
}

private data class ElapsedClockSnapshot(
    val label: String,
    val contentDescription: String,
    val delayUntilNextSecond: Duration,
)

private fun LocalDateTime.clockSnapshot(now: LocalDateTime): ElapsedClockSnapshot {
    val elapsed = elapsedSince(this, now)
    val nanosPerSecond = 1.seconds.inWholeNanoseconds
    val nanosIntoSecond = elapsed.inWholeNanoseconds % nanosPerSecond
    val nanosUntilNextSecond = if (nanosIntoSecond == 0L) {
        nanosPerSecond
    } else {
        nanosPerSecond - nanosIntoSecond
    }
    return ElapsedClockSnapshot(
        label = elapsed.formatElapsedClock(),
        contentDescription = elapsed.elapsedClockContentDescription(),
        delayUntilNextSecond = nanosUntilNextSecond.nanoseconds.coerceAtLeast(1.milliseconds),
    )
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
