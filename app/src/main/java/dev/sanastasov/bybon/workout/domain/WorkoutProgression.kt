package dev.sanastasov.bybon.workout.domain

fun WorkoutSession.adjustAll(increase: Boolean): WorkoutSession =
    copy(exercises = exercises.map { it.adjust(increase) })

fun WorkoutSession.adjustExercise(exercise: WorkoutExercise, increase: Boolean): WorkoutSession =
    updateExercise(exercise.id) { it.adjust(increase) }

fun WorkoutSession.resetTo(previous: WorkoutSession): WorkoutSession = previous

fun WorkoutSession.resetExercise(
    exercise: WorkoutExercise,
    previous: WorkoutSession,
): WorkoutSession {
    val restored = previous.exercises.firstOrNull { it.id == exercise.id } ?: return this
    return updateExercise(exercise.id) { restored }
}

private fun WorkoutExercise.adjust(increase: Boolean): WorkoutExercise {
    val originalFirst = sets.firstOrNull() ?: return this
    val increment = exerciseDefinition.equipment?.weightIncrement
    val adjustedFirst = originalFirst.adjust(repRange, increment, increase)
    return copy(
        sets = listOf(adjustedFirst) + sets.drop(1).map { set ->
            set.followFirstWorkSet(originalFirst, adjustedFirst, repRange, increase)
        },
    )
}
