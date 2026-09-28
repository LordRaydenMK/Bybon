package dev.sanastasov.bybon.workout.domain

internal const val DEFAULT_ADDED_WORK_SETS = 3
internal val DEFAULT_ADDED_REP_RANGE = 8..12

fun WorkoutPlan.addWorkSet(exerciseId: String): WorkoutPlan =
    updatePlannedExercise(exerciseId) { exercise ->
        exercise.copy(sets = exercise.sets + 1)
    }

fun WorkoutPlan.removeLastWorkSet(exerciseId: String): WorkoutPlan =
    updatePlannedExercise(exerciseId) { exercise ->
        check(exercise.sets > 1) {
            "Cannot remove last work set from $exerciseId; only one remains"
        }
        exercise.copy(sets = exercise.sets - 1)
    }

fun WorkoutPlan.removeExercise(exerciseId: String): WorkoutPlan {
    val exercise = requireExercise(exerciseId)
    check(sets.size > 1) {
        "Cannot remove last exercise from the plan"
    }
    return copy(sets = sets.filter { it.exercise.id != exercise.exercise.id })
}

fun WorkoutPlan.addExercise(exercise: ExerciseDefinition): WorkoutPlan {
    check(sets.none { it.exercise.id == exercise.id }) {
        "Exercise ${exercise.id} is already on the plan"
    }
    return copy(
        sets = sets + PlanedExercise(
            exercise = exercise,
            sets = DEFAULT_ADDED_WORK_SETS,
            repRange = DEFAULT_ADDED_REP_RANGE,
        ),
    )
}

fun WorkoutPlan.moveExerciseUp(exerciseId: String): WorkoutPlan =
    moveExercise(exerciseId, offset = -1)

fun WorkoutPlan.moveExerciseDown(exerciseId: String): WorkoutPlan =
    moveExercise(exerciseId, offset = 1)

private fun WorkoutPlan.moveExercise(exerciseId: String, offset: Int): WorkoutPlan {
    requireExercise(exerciseId)
    val fromIndex = sets.indexOfFirst { it.exercise.id == exerciseId }
    val toIndex = fromIndex + offset
    check(toIndex in sets.indices) {
        val direction = if (offset < 0) "up" else "down"
        "Cannot move $exerciseId $direction"
    }
    return copy(
        sets = sets.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        },
    )
}

private fun WorkoutPlan.updatePlannedExercise(
    exerciseId: String,
    transform: (PlanedExercise) -> PlanedExercise,
): WorkoutPlan {
    requireExercise(exerciseId)
    return copy(
        sets = sets.map { exercise ->
            if (exercise.exercise.id == exerciseId) transform(exercise) else exercise
        },
    )
}

private fun WorkoutPlan.requireExercise(exerciseId: String): PlanedExercise =
    sets.firstOrNull { it.exercise.id == exerciseId }
        ?: error("Exercise $exerciseId is not in the plan")
