package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime
import kotlin.time.Duration

data class RestTimer(
    val startedAt: LocalDateTime,
    val duration: Duration,
) {
    fun remaining(now: LocalDateTime): Duration =
        (duration - elapsedSince(startedAt, now)).coerceAtLeast(Duration.ZERO)

    fun progress(now: LocalDateTime): Float? {
        val elapsed = elapsedSince(startedAt, now)
        return when {
            duration <= Duration.ZERO || elapsed >= duration -> null
            else -> (elapsed / duration).toFloat()
        }
    }
}

fun WorkoutSession.restTimer(): RestTimer? {
    val sets = setsInOrder()
    val lastCompletedIndex = sets.indexOfLast { it.set.setState is SetState.Completed }
    val completed = sets.getOrNull(lastCompletedIndex)
        ?.takeIf { lastCompletedIndex != sets.lastIndex && it.startsRest }
    val completedAt = completed?.set?.completedAt
    val duration = completed?.restAfterWorkSet
    return if (completedAt != null && duration != null && duration > Duration.ZERO) {
        RestTimer(completedAt, duration)
    } else {
        null
    }
}

private data class OrderedSessionSet(
    val set: ExerciseSet,
    val startsRest: Boolean,
    val restAfterWorkSet: Duration,
)

private fun WorkoutSession.setsInOrder(): List<OrderedSessionSet> = exercises.flatMap { exercise ->
    val lastWarmupIndex = exercise.warmupSets?.lastIndex
    buildList {
        exercise.numberedWarmupSets?.forEach { numbered ->
            add(
                OrderedSessionSet(
                    set = numbered.set,
                    startsRest = numbered.index == lastWarmupIndex,
                    restAfterWorkSet = exercise.restAfterWorkSet,
                ),
            )
        }
        exercise.numberedWorkSets.forEach { numbered ->
            add(
                OrderedSessionSet(
                    set = numbered.set,
                    startsRest = true,
                    restAfterWorkSet = exercise.restAfterWorkSet,
                ),
            )
        }
    }
}
