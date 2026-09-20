package dev.sanastasov.bybon.workout.domain

fun WorkoutSession.addExercise(exercise: ExerciseDefinition): WorkoutSession {
    check(exercises.none { it.id == exercise.id }) {
        "Exercise ${exercise.id} is already in the session"
    }
    return copy(exercises = exercises + exercise.toAddedWorkoutExercise())
}

private fun ExerciseDefinition.toAddedWorkoutExercise(): WorkoutExercise = WorkoutExercise(
    exerciseDefinition = this,
    repRange = DEFAULT_ADDED_REP_RANGE,
    warmupSets = null,
    sets = List(DEFAULT_ADDED_WORK_SETS) {
        ExerciseSet(
            exerciseDefinition = this,
            weight = Weight.kilograms(50),
            reps = DEFAULT_ADDED_REP_RANGE.first,
            setState = SetState.NotStated,
        )
    },
)
