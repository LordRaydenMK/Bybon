package dev.sanastasov.bybon.workout.domain

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
    return copy(sets = sets.filter { it.exercise.id != exercise.exercise.id })
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
