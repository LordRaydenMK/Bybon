package dev.sanastasov.bybon.workout.domain

fun WorkoutSession.updateExercise(
    exerciseId: String,
    update: (WorkoutExercise) -> WorkoutExercise,
): WorkoutSession = copy(
    exercises = exercises.map { exercise ->
        if (exercise.id == exerciseId) {
            update(exercise)
        } else {
            exercise
        }
    },
)

fun WorkoutSession.updateExerciseSet(
    exercise: WorkoutExercise,
    setIndex: Int,
    isWarmup: Boolean = false,
    update: (ExerciseSet) -> ExerciseSet,
): WorkoutSession = updateExercise(exercise.id) { current ->
    if (isWarmup) {
        current.copy(
            warmupSets = current.warmupSets?.mapIndexed { index, set ->
                if (index == setIndex) update(set) else set
            },
        )
    } else {
        current.copy(
            sets = current.sets.mapIndexed { index, set ->
                if (index == setIndex) update(set) else set
            },
        )
    }
}
