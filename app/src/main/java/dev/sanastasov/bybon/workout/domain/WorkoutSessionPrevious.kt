package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime

fun WorkoutExercise.withPreviousFrom(previous: WorkoutExercise?): WorkoutExercise = copy(
    warmupSets = warmupSets?.mapIndexed { index, set ->
        set.copy(previous = previous?.warmupSets?.getOrNull(index)?.toPreviousPerformance())
    },
    sets = sets.mapIndexed { index, set ->
        set.copy(previous = previous?.sets?.getOrNull(index)?.toPreviousPerformance())
    },
)

fun WorkoutSession.withPreviousFromHistory(history: List<WorkoutSession>): WorkoutSession = copy(
    exercises = exercises.map { exercise ->
        exercise.withPreviousFrom(
            history.latestCompletedExercise(exercise.id, before = startedAt),
        )
    },
)

fun List<WorkoutSession>.latestCompletedExercise(
    exerciseId: String,
    before: LocalDateTime? = null,
): WorkoutExercise? = mapNotNull { session ->
    if (session.state !is WorkoutState.Completed) {
        null
    } else if (before != null && !session.startedAt.isBefore(before)) {
        null
    } else {
        session.exercises.firstOrNull { it.id == exerciseId }?.let { session.startedAt to it }
    }
}.maxByOrNull { it.first }?.second

private fun ExerciseSet.toPreviousPerformance(): PreviousSetPerformance =
    PreviousSetPerformance(weight, reps)
