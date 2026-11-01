package dev.sanastasov.bybon.workout

import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.completeSet
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.restTimer
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import dev.sanastasov.bybon.workout.ui.weightRepsLabel
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.time.toJavaDuration
import org.junit.Test

class WorkoutSessionNotificationTest {

    private val startedAt = LocalDateTime.of(2026, 10, 24, 12, 0, 0)
    private val zone = ZoneOffset.UTC

    @Test
    fun `started session uses elapsed chronometer and warmup fraction`() {
        val session = fullBodyA.toWorkoutSession(startedAt = startedAt)
        val inProgress = checkNotNull(session.inProgressSet)
        val content = session.toNotificationContent(startedAt, zone)

        assert(content.planName == session.planName)
        assert(content.contentText == "[w] 1/3 Barbell Bench Press ${inProgress.weightRepsLabel}")
        assert(content.chronometerCountDown == false)
        assert(content.whenTime == startedAt.atZone(zone).toInstant())
        assert(content.elapsedStart == startedAt.atZone(zone).toInstant())
        assert(content.restEnd == null)
        assert(content.currentSetIsWarmup)
        assert(content.currentSetNumber == 1)
        assert(content.currentSetTotal == 3)
        assert(content.currentSetValue == inProgress.weightRepsLabel)
        assert(
            content.metrics == listOf(
                WorkoutSessionNotificationMetric.Elapsed,
                WorkoutSessionNotificationMetric.Set,
            ),
        )
        assert(content.criticalMetricIndex == 0)
        assert(content.showCompleteSetAction)
    }

    @Test
    fun `second warmup has no rest and uses warmup fraction`() {
        val started = fullBodyA.toWorkoutSession(startedAt = startedAt)
        val session = started.completeSet(
            started.exercises.first(),
            0,
            isWarmup = true,
            now = startedAt,
        )
        val inProgress = checkNotNull(session.inProgressSet)
        val content = session.toNotificationContent(startedAt.plusMinutes(1), zone)

        assert(content.contentText == "[w] 2/3 Barbell Bench Press ${inProgress.weightRepsLabel}")
        assert(content.chronometerCountDown == false)
        assert(content.whenTime == startedAt.atZone(zone).toInstant())
        assert(content.restEnd == null)
        assert(content.currentSetIsWarmup)
        assert(content.currentSetNumber == 2)
        assert(content.currentSetTotal == 3)
        assert(content.currentSetValue == inProgress.weightRepsLabel)
        assert(
            content.metrics == listOf(
                WorkoutSessionNotificationMetric.Elapsed,
                WorkoutSessionNotificationMetric.Set,
            ),
        )
        assert(content.showCompleteSetAction)
    }

    @Test
    fun `active rest uses countdown to rest end and keeps current set metric`() {
        val completedAt = startedAt.plusMinutes(4)
        val session = fullBodyA.toWorkoutSession(startedAt = startedAt)
            .completeWarmups(completedAt)
        val inProgress = checkNotNull(session.inProgressSet)
        val rest = checkNotNull(session.restTimer())
        val restEnd = rest.startedAt.plus(rest.duration.toJavaDuration())
        val content = session.toNotificationContent(completedAt.plusMinutes(1), zone)

        assert(content.chronometerCountDown)
        assert(content.whenTime == restEnd.atZone(zone).toInstant())
        assert(content.restEnd == restEnd.atZone(zone).toInstant())
        assert(content.elapsedStart == startedAt.atZone(zone).toInstant())
        assert(content.currentSetIsWarmup == false)
        assert(content.currentSetNumber == 1)
        assert(content.currentSetTotal == 3)
        assert(content.currentSetValue == inProgress.weightRepsLabel)
        assert(content.contentText == "1/3 Barbell Bench Press ${inProgress.weightRepsLabel}")
        assert(
            content.metrics == listOf(
                WorkoutSessionNotificationMetric.Rest,
                WorkoutSessionNotificationMetric.Set,
            ),
        )
        assert(content.criticalMetricIndex == 0)
        assert(content.showCompleteSetAction == false)
    }

    @Test
    fun `expired rest switches back to elapsed chronometer`() {
        val completedAt = startedAt.plusMinutes(4)
        val session = fullBodyA.toWorkoutSession(startedAt = startedAt)
            .completeWarmups(completedAt)
        val content = session.toNotificationContent(completedAt.plusMinutes(2), zone)

        assert(content.chronometerCountDown == false)
        assert(content.whenTime == startedAt.atZone(zone).toInstant())
        assert(content.restEnd == null)
        assert(content.currentSetIsWarmup == false)
        assert(content.currentSetNumber == 1)
        assert(content.currentSetTotal == 3)
        assert(
            content.metrics == listOf(
                WorkoutSessionNotificationMetric.Elapsed,
                WorkoutSessionNotificationMetric.Set,
            ),
        )
        assert(content.showCompleteSetAction)
    }
}

private fun WorkoutSession.completeWarmups(now: LocalDateTime): WorkoutSession {
    val warmupCount = exercises.first().warmupSets?.size ?: 0
    return (0 until warmupCount).fold(this) { session, index ->
        session.completeSet(session.exercises.first(), index, isWarmup = true, now = now)
    }
}
