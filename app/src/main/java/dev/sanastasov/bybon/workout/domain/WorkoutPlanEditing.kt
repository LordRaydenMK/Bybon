package dev.sanastasov.bybon.workout.domain

fun WorkoutPlan.addWorkSet(exerciseId: String): WorkoutPlan =
    updatePlannedExercise(exerciseId) { exercise ->
        exercise.copy(sets = exercise.sets + 1)
    }

fun WorkoutPlan.removeLastWorkSet(exerciseId: String): WorkoutPlan =
    updatePlannedExercise(exerciseId) { exercise ->
        if (exercise.sets <= 1) exercise else exercise.copy(sets = exercise.sets - 1)
    }

fun WorkoutPlan.removeExercise(exerciseId: String): WorkoutPlan =
    copy(sets = sets.filter { it.exercise.id != exerciseId })

private fun WorkoutPlan.updatePlannedExercise(
    exerciseId: String,
    transform: (PlanedExercise) -> PlanedExercise,
): WorkoutPlan = copy(
    sets = sets.map { exercise ->
        if (exercise.exercise.id == exerciseId) transform(exercise) else exercise
    },
)
