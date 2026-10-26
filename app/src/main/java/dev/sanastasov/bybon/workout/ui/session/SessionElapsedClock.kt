package dev.sanastasov.bybon.workout.ui.session

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun SessionElapsedClock(elapsed: Duration, modifier: Modifier = Modifier) {
    Text(
        elapsed.formatSessionElapsedClock(),
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

internal fun Duration.formatSessionElapsedClock(): String {
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
    SessionElapsedClock(50.minutes + 20.seconds)
}
