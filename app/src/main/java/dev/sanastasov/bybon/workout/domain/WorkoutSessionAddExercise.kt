package dev.sanastasov.bybon.workout.domain

fun WorkoutSession.addExercise(exercise: ExerciseDefinition): WorkoutSession {
    check(exercises.none { it.id == exercise.id }) {
        "Exercise ${exercise.id} is already in the session"
    }
    return copy(exercises = exercises + exercise.toAddedWorkoutExercise())
}

@Suppress("ReturnCount")
fun WorkoutSession.removeExercise(exerciseId: String): WorkoutSession {
    exercises.firstOrNull { it.id == exerciseId }
        ?: error("Exercise $exerciseId is not in the session")
    check(exercises.size > 1) {
        "Cannot remove last exercise from the session"
    }
    val hadInProgress = workoutSets.any { it.setState == SetState.InProgress }
    val updated = copy(exercises = exercises.filter { it.id != exerciseId })
    if (!hadInProgress || updated.workoutSets.any { it.setState == SetState.InProgress }) {
        return updated
    }
    val next = updated.firstNotStartedSet() ?: return updated
    val nextExercise = updated.exercises.first { it.id == next.exerciseId }
    return updated.updateExerciseSet(nextExercise, next.index, next.isWarmup) {
        it.copy(setState = SetState.InProgress)
    }
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
