package dev.sanastasov.bybon.workout.domain

fun WorkoutSession.addExercise(exercise: ExerciseDefinition): WorkoutSession {
    check(exercises.none { it.id == exercise.id }) {
        "Exercise ${exercise.id} is already in the session"
    }
    return copy(exercises = exercises + exercise.toAddedWorkoutExercise())
}

@Suppress("ReturnCount")
fun WorkoutSession.removeExercise(exerciseId: String): WorkoutSession {
    val exercise = exercises.firstOrNull { it.id == exerciseId }
        ?: error("Exercise $exerciseId is not in the session")
    check(exercises.size > 1) {
        "Cannot remove last exercise from the session"
    }
    check(exercise.state != ExerciseState.Completed) {
        "Cannot remove completed exercise $exerciseId"
    }
    val hadInProgress = workoutSets.any { it.setState == SetState.InProgress }
    val updated = copy(exercises = exercises.filter { it.id != exerciseId })
    if (!hadInProgress || updated.workoutSets.any { it.setState == SetState.InProgress }) {
        return updated.withDurationIfCompleted()
    }
    val next = updated.firstNotStartedSet() ?: return updated.withDurationIfCompleted()
    val nextExercise = updated.exercises.first { it.id == next.exerciseId }
    return updated.updateExerciseSet(nextExercise, next.index, next.isWarmup) {
        it.copy(setState = SetState.InProgress)
    }
}

fun WorkoutSession.canRemoveExercise(exercise: WorkoutExercise): Boolean = exercises.size > 1 &&
    exercise.state != ExerciseState.Completed &&
    exercises.any { it.id == exercise.id }

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
