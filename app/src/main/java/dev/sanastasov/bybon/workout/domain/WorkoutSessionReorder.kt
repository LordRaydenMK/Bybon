package dev.sanastasov.bybon.workout.domain

fun WorkoutSession.moveExerciseUp(exerciseId: String): WorkoutSession =
    moveExercise(exerciseId, ReorderDirection.Up)

fun WorkoutSession.moveExerciseDown(exerciseId: String): WorkoutSession =
    moveExercise(exerciseId, ReorderDirection.Down)

fun WorkoutSession.moveDirections(exercise: WorkoutExercise): List<ReorderDirection> {
    val fromIndex = exercises.indexOfFirst { it.id == exercise.id }
    if (fromIndex < 0 || exercises[fromIndex].state == ExerciseState.Completed) {
        return emptyList()
    }
    return ReorderDirection.entries.filter { direction ->
        val toIndex = fromIndex + direction.offset
        toIndex in exercises.indices && exercises[toIndex].state != ExerciseState.Completed
    }
}

fun WorkoutSession.moveExercise(exerciseId: String, direction: ReorderDirection): WorkoutSession {
    val exercise = requireExercise(exerciseId)
    require(exercise.state != ExerciseState.Completed) {
        "Cannot move completed exercise $exerciseId"
    }
    require(direction in moveDirections(exercise)) {
        "Cannot move $exerciseId ${direction.name.lowercase()}"
    }
    val fromIndex = exercises.indexOfFirst { it.id == exerciseId }
    val toIndex = fromIndex + direction.offset
    return copy(
        exercises = exercises.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        },
    )
}

private fun WorkoutSession.requireExercise(exerciseId: String): WorkoutExercise =
    requireNotNull(exercises.firstOrNull { it.id == exerciseId }) {
        "Exercise $exerciseId is not in the session"
    }
