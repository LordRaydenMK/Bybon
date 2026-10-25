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

fun startsRestAfter(
    isWarmup: Boolean,
    index: Int,
    warmupCount: Int,
    workCount: Int,
    isLastExercise: Boolean,
): Boolean {
    if (!isWarmup && isLastExercise && index == workCount - 1) return false
    return if (isWarmup) warmupCount > 0 && index == warmupCount - 1 else true
}

fun WorkoutExercise.startsRestAfter(
    isWarmup: Boolean,
    index: Int,
    isLastExercise: Boolean,
): Boolean = startsRestAfter(
    isWarmup = isWarmup,
    index = index,
    warmupCount = warmupSets?.size ?: 0,
    workCount = sets.size,
    isLastExercise = isLastExercise,
)

fun WorkoutExercise.restAfter(isWarmup: Boolean, index: Int, isLastExercise: Boolean): Duration? =
    restAfterWorkSet.takeIf { startsRestAfter(isWarmup, index, isLastExercise) }

fun PlanedExercise.startsRestAfter(
    isWarmup: Boolean,
    index: Int,
    isLastExercise: Boolean,
): Boolean = startsRestAfter(
    isWarmup = isWarmup,
    index = index,
    warmupCount = warmupSets,
    workCount = sets,
    isLastExercise = isLastExercise,
)

fun PlanedExercise.restAfter(isWarmup: Boolean, index: Int, isLastExercise: Boolean): Duration? =
    restAfterWorkSet.takeIf { startsRestAfter(isWarmup, index, isLastExercise) }

fun WorkoutSession.restTimer(): RestTimer? {
    val sets = setsInOrder()
    val lastCompletedIndex = sets.indexOfLast { it.set.setState is SetState.Completed }
    val completed = sets.getOrNull(lastCompletedIndex)?.takeIf { it.startsRest }
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

private fun WorkoutSession.setsInOrder(): List<OrderedSessionSet> =
    exercises.flatMapIndexed { exerciseIndex, exercise ->
        val isLastExercise = exerciseIndex == exercises.lastIndex
        buildList {
            exercise.numberedWarmupSets?.forEach { numbered ->
                add(
                    OrderedSessionSet(
                        set = numbered.set,
                        startsRest = exercise.startsRestAfter(
                            isWarmup = true,
                            index = numbered.index,
                            isLastExercise = isLastExercise,
                        ),
                        restAfterWorkSet = exercise.restAfterWorkSet,
                    ),
                )
            }
            exercise.numberedWorkSets.forEach { numbered ->
                add(
                    OrderedSessionSet(
                        set = numbered.set,
                        startsRest = exercise.startsRestAfter(
                            isWarmup = false,
                            index = numbered.index,
                            isLastExercise = isLastExercise,
                        ),
                        restAfterWorkSet = exercise.restAfterWorkSet,
                    ),
                )
            }
        }
    }
