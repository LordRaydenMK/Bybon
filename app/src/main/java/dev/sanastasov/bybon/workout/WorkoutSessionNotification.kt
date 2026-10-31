package dev.sanastasov.bybon.workout

import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.activeRestTimer
import dev.sanastasov.bybon.workout.domain.inProgressSetGroup
import dev.sanastasov.bybon.workout.ui.notificationText
import dev.sanastasov.bybon.workout.ui.weightRepsLabel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.time.toJavaDuration

internal const val WORKOUT_SESSION_CHANNEL_ID = "workout_session_v2"
internal const val REST_COMPLETE_CHANNEL_ID = "rest_complete"
internal const val LEGACY_WORKOUT_SESSION_CHANNEL_ID = "workout_session"
internal const val WORKOUT_SESSION_NOTIFICATION_ID = 1001

enum class WorkoutSessionNotificationMetric {
    Elapsed,
    Rest,
    Set,
}

enum class WorkoutSessionNotificationKind {
    Ongoing,
    RestComplete,
}

data class WorkoutSessionNotificationPresentation(
    val channelId: String,
    val silent: Boolean,
    val onlyAlertOnce: Boolean,
    val restOverTitle: Boolean,
)

fun WorkoutSessionNotificationKind.toPresentation(): WorkoutSessionNotificationPresentation =
    when (this) {
        WorkoutSessionNotificationKind.Ongoing -> WorkoutSessionNotificationPresentation(
            channelId = WORKOUT_SESSION_CHANNEL_ID,
            silent = true,
            onlyAlertOnce = true,
            restOverTitle = false,
        )

        WorkoutSessionNotificationKind.RestComplete -> WorkoutSessionNotificationPresentation(
            channelId = REST_COMPLETE_CHANNEL_ID,
            silent = false,
            onlyAlertOnce = false,
            restOverTitle = true,
        )
    }

data class WorkoutSessionNotification(
    val planName: String,
    val contentText: String,
    val chronometerCountDown: Boolean,
    val elapsedStart: Instant,
    val restEnd: Instant?,
    val currentSetNumber: Int,
    val currentSetTotal: Int,
    val currentSetIsWarmup: Boolean,
    val currentSetValue: String,
) {
    val whenTime: Instant
        get() = restEnd ?: elapsedStart

    val metrics: List<WorkoutSessionNotificationMetric>
        get() = if (restEnd != null) {
            listOf(WorkoutSessionNotificationMetric.Rest, WorkoutSessionNotificationMetric.Set)
        } else {
            listOf(WorkoutSessionNotificationMetric.Elapsed, WorkoutSessionNotificationMetric.Set)
        }

    val criticalMetricIndex: Int
        get() = 0
}

fun WorkoutSession.toNotificationContent(
    now: LocalDateTime,
    zone: ZoneId = ZoneId.systemDefault(),
): WorkoutSessionNotification {
    val group = checkNotNull(inProgressSetGroup()) {
        "Active session must have an in-progress set"
    }
    val elapsedStart = startedAt.atZone(zone).toInstant()
    val restEnd = activeRestTimer(now)?.let { timer ->
        timer.startedAt.plus(timer.duration.toJavaDuration()).atZone(zone).toInstant()
    }
    return WorkoutSessionNotification(
        planName = planName,
        contentText = group.set.notificationText(group.totalInGroup),
        chronometerCountDown = restEnd != null,
        elapsedStart = elapsedStart,
        restEnd = restEnd,
        currentSetNumber = group.set.setNumber,
        currentSetTotal = group.totalInGroup,
        currentSetIsWarmup = group.set.isWarmup,
        currentSetValue = group.set.weightRepsLabel,
    )
}
