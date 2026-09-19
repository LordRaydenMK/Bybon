package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.toKotlinDuration

fun WorkoutSession.completeSet(
    exercise: WorkoutExercise,
    setIndex: Int,
    isWarmup: Boolean = false,
    now: LocalDateTime = LocalDateTime.now(),
): WorkoutSession {
    val next = nextSetAfter(exercise.id, setIndex, isWarmup)
    val updated = updateExerciseSet(exercise, setIndex, isWarmup) {
        it.copy(setState = SetState.Completed)
    }
    val target = next ?: updated.firstNotStartedSet()
        ?: return updated.withDurationIfCompleted(now)
    val nextExercise = updated.exercises.first { it.id == target.exerciseId }
    return updated.updateExerciseSet(nextExercise, target.index, target.isWarmup) {
        it.copy(setState = SetState.InProgress)
    }
}

data class SetRef(
    val exerciseId: String,
    val index: Int,
    val isWarmup: Boolean,
)

private fun WorkoutSession.nextSetAfter(
    exerciseId: String,
    setIndex: Int,
    isWarmup: Boolean,
): SetRef? {
    val exercise = exercises.first { it.id == exerciseId }
    val nextInExercise = if (isWarmup) {
        nextSetAfterWarmup(exercise, setIndex)
    } else {
        nextSetAfterWork(exercise, setIndex)
    }
    return nextInExercise ?: firstSetOfNextExercise(exerciseId)
}

private fun nextSetAfterWarmup(exercise: WorkoutExercise, setIndex: Int): SetRef? {
    val warmupSets = exercise.warmupSets
    return when {
        warmupSets != null && setIndex < warmupSets.lastIndex ->
            SetRef(exercise.id, setIndex + 1, isWarmup = true)

        exercise.sets.isNotEmpty() ->
            SetRef(exercise.id, 0, isWarmup = false)

        else -> null
    }
}

private fun nextSetAfterWork(exercise: WorkoutExercise, setIndex: Int): SetRef? =
    if (setIndex < exercise.sets.lastIndex) {
        SetRef(exercise.id, setIndex + 1, isWarmup = false)
    } else {
        null
    }

private fun WorkoutSession.firstSetOfNextExercise(exerciseId: String): SetRef? {
    val nextExercise = exercises.getOrNull(exercises.indexOfFirst { it.id == exerciseId } + 1)
        ?: return null
    return when {
        nextExercise.warmupSets != null -> SetRef(nextExercise.id, 0, isWarmup = true)
        nextExercise.sets.isNotEmpty() -> SetRef(nextExercise.id, 0, isWarmup = false)
        else -> null
    }
}

fun WorkoutSession.withDurationIfCompleted(
    now: LocalDateTime = LocalDateTime.now(),
): WorkoutSession {
    if (duration != null || state !is WorkoutState.Completed) return this
    val elapsed = java.time.Duration.between(startedAt, now).toKotlinDuration()
    return copy(duration = elapsed.coerceAtLeast(Duration.ZERO))
}

fun WorkoutSession.firstNotStartedSet(): SetRef? = exercises.firstNotNullOfOrNull { exercise ->
    val warmupIndex = exercise.warmupSets?.indexOfFirst { it.setState == SetState.NotStated }
    when {
        warmupIndex != null && warmupIndex >= 0 ->
            SetRef(exercise.id, warmupIndex, isWarmup = true)

        else -> {
            val workIndex = exercise.sets.indexOfFirst { it.setState == SetState.NotStated }
            if (workIndex >= 0) SetRef(exercise.id, workIndex, isWarmup = false) else null
        }
    }
}
