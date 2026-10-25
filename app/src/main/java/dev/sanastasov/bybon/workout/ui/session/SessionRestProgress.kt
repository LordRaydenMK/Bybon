package dev.sanastasov.bybon.workout.ui.session

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sanastasov.bybon.workout.domain.RestTimer
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.map

@Composable
internal fun SessionRestProgress(
    rest: RestTimer,
    modifier: Modifier = Modifier,
    now: () -> LocalDateTime = LocalDateTime::now,
) {
    val progress by remember(rest, now) {
        ticksEverySecond()
            .map { rest.progressUi(now()) }
            .takeUntil { it == null }
    }.collectAsStateWithLifecycle(
        initialValue = rest.progressUi(now()),
        minActiveState = Lifecycle.State.RESUMED,
    )
    val snapshot = progress ?: return
    LinearProgressIndicator(
        progress = { snapshot.fraction },
        modifier = modifier
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

@Composable
internal fun ActivateNextSetWhenRestElapsed(
    rest: RestTimer,
    onElapsed: () -> Unit,
    now: () -> LocalDateTime = LocalDateTime::now,
) {
    val remaining by remember(rest, now) {
        ticksEverySecond().map { rest.remaining(now()) }
    }.collectAsStateWithLifecycle(
        initialValue = rest.remaining(now()),
        minActiveState = Lifecycle.State.RESUMED,
    )
    LaunchedEffect(remaining) {
        if (remaining == Duration.ZERO) onElapsed()
    }
}

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

@Preview
@Composable
private fun SessionRestProgressPreview() {
    val startedAt = LocalDateTime.of(2026, 10, 24, 12, 0, 0)
    SessionRestProgress(
        RestTimer(startedAt, 2.minutes),
        now = { startedAt.plusSeconds(45) },
    )
}
