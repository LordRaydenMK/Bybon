package dev.sanastasov.bybon.workout.ui.session

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import dev.sanastasov.bybon.workout.domain.OpenWorkoutSnapshot
import dev.sanastasov.bybon.workout.domain.RestTimer
import dev.sanastasov.bybon.workout.domain.restTimer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun SessionRestProgress(progress: RestProgressUi, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress.fraction },
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = progress.remaining.restRemainingContentDescription()
            },
    )
}

internal data class RestProgressUi(
    val fraction: Float,
    val remaining: Duration,
)

internal fun OpenWorkoutSnapshot.restProgressUi(): RestProgressUi? {
    val remaining = restRemaining
    val timer = session.restTimer()
    return if (remaining == null || timer == null) null else timer.progressUi(remaining)
}

internal fun RestTimer.progressUi(remaining: Duration): RestProgressUi? {
    if (remaining <= Duration.ZERO || duration <= Duration.ZERO) return null
    val elapsed = duration - remaining
    return RestProgressUi((elapsed / duration).toFloat(), remaining)
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

@Preview
@Composable
private fun SessionRestProgressPreview() {
    SessionRestProgress(
        RestProgressUi(fraction = 0.375f, remaining = 2.minutes - 45.seconds),
    )
}
