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

data class WorkoutSessionNotification(
    val planName: String,
    val contentText: String,
    val whenMillis: Long,
    val chronometerCountDown: Boolean,
    val elapsedStart: Instant,
    val restEnd: Instant?,
    val currentSetNumber: Int,
    val currentSetTotal: Int,
    val currentSetIsWarmup: Boolean,
    val currentSetValue: String,
)

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
    val whenInstant = restEnd ?: elapsedStart
    return WorkoutSessionNotification(
        planName = planName,
        contentText = group.set.notificationText(group.totalInGroup),
        whenMillis = whenInstant.toEpochMilli(),
        chronometerCountDown = restEnd != null,
        elapsedStart = elapsedStart,
        restEnd = restEnd,
        currentSetNumber = group.set.setNumber,
        currentSetTotal = group.totalInGroup,
        currentSetIsWarmup = group.set.isWarmup,
        currentSetValue = group.set.weightRepsLabel,
    )
}
