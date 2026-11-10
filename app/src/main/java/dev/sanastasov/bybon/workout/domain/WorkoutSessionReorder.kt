package dev.sanastasov.bybon.workout.domain

fun WorkoutSession.moveExerciseUp(exerciseId: String): WorkoutSession =
    moveExercise(exerciseId, offset = -1)

fun WorkoutSession.moveExerciseDown(exerciseId: String): WorkoutSession =
    moveExercise(exerciseId, offset = 1)

fun WorkoutSession.canMoveExerciseUp(exercise: WorkoutExercise): Boolean =
    canMoveExercise(exercise.id, offset = -1)

fun WorkoutSession.canMoveExerciseDown(exercise: WorkoutExercise): Boolean =
    canMoveExercise(exercise.id, offset = 1)

private fun WorkoutSession.moveExercise(exerciseId: String, offset: Int): WorkoutSession {
    val exercise = requireExercise(exerciseId)
    check(exercise.state != ExerciseState.Completed) {
        "Cannot move completed exercise $exerciseId"
    }
    val fromIndex = exercises.indexOfFirst { it.id == exerciseId }
    val toIndex = fromIndex + offset
    check(toIndex in exercises.indices && exercises[toIndex].state != ExerciseState.Completed) {
        val direction = if (offset < 0) "up" else "down"
        "Cannot move $exerciseId $direction"
    }
    return copy(
        exercises = exercises.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        },
    )
}

private fun WorkoutSession.canMoveExercise(exerciseId: String, offset: Int): Boolean {
    val fromIndex = exercises.indexOfFirst { it.id == exerciseId }
    if (fromIndex < 0 || exercises[fromIndex].state == ExerciseState.Completed) return false
    val toIndex = fromIndex + offset
    return toIndex in exercises.indices && exercises[toIndex].state != ExerciseState.Completed
}

private fun WorkoutSession.requireExercise(exerciseId: String): WorkoutExercise =
    exercises.firstOrNull { it.id == exerciseId }
        ?: error("Exercise $exerciseId is not in the session")
