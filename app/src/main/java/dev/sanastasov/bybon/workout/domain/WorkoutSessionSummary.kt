package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime

data class WorkoutSessionSummary(
    val exerciseCount: Int,
    val workingSetCount: Int,
    val totalReps: Int,
    val volumeLoadKg: Float,
    val setPerformance: PerformanceChange,
    val exercisePerformance: List<ExerciseAverageOneRmChange>,
    val musclesTrained: List<TrainedMuscle>,
)

data class PerformanceChange(
    val improved: Int,
    val maintained: Int,
    val declined: Int,
) {
    val comparableCount: Int get() = improved + maintained + declined
}

data class ExerciseAverageOneRmChange(
    val exerciseId: String,
    val name: String,
    val currentAverage: Weight,
    val previousAverage: Weight,
)

fun WorkoutSession.workoutSessionSummary(
    completedSessions: List<WorkoutSession>,
): WorkoutSessionSummary {
    val matched = exercises.map { exercise ->
        val previous = completedSessions.latestCompletedExercise(exercise.id, before = startedAt)
        if (previous != null) exercise.withPreviousFrom(previous) else exercise
    }
    val workingSets = matched.flatMap { it.sets }
    return WorkoutSessionSummary(
        exerciseCount = matched.size,
        workingSetCount = workingSets.size,
        totalReps = workingSets.sumOf { it.reps },
        volumeLoadKg = workingSets.volumeLoadKg(),
        setPerformance = workingSets.setPerformance(),
        exercisePerformance = matched.mapNotNull { exercise ->
            exercise.averageOneRmChange(completedSessions, startedAt)
        },
        musclesTrained = matched
            .flatMap { it.exerciseDefinition.trainedMuscles() }
            .toSet()
            .sortedBy { it.ordinal },
    )
}

private fun List<ExerciseSet>.volumeLoadKg(): Float = sumOf { set ->
    (set.weight?.kilogramsValue ?: 0f).toDouble() * set.reps
}.toFloat()

private fun List<ExerciseSet>.setPerformance(): PerformanceChange {
    var improved = 0
    var maintained = 0
    var declined = 0
    forEach { set ->
        val current = set.oneRm ?: return@forEach
        val previous = set.previousOneRm ?: return@forEach
        when {
            current > previous -> improved++
            current < previous -> declined++
            else -> maintained++
        }
    }
    return PerformanceChange(improved, maintained, declined)
}

private fun WorkoutExercise.averageOneRmChange(
    completedSessions: List<WorkoutSession>,
    sessionStartedAt: LocalDateTime,
): ExerciseAverageOneRmChange? {
    val currentAverage = sets.mapNotNull { it.oneRm }.averageWeight()
    val previousAverage = completedSessions.latestCompletedExercise(id, before = sessionStartedAt)
        ?.sets
        ?.mapNotNull { it.oneRm }
        ?.averageWeight()
    if (currentAverage == null || previousAverage == null) return null
    return ExerciseAverageOneRmChange(
        exerciseId = id,
        name = exerciseDefinition.name,
        currentAverage = currentAverage,
        previousAverage = previousAverage,
    )
}

private fun List<Weight>.averageWeight(): Weight? {
    if (isEmpty()) return null
    return Weight.kilograms(map { it.kilogramsValue.toDouble() }.average().toFloat())
}
