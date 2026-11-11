package dev.sanastasov.bybon.workout.domain

import java.time.LocalDate
import kotlin.time.Duration

data class LastSevenDaysSummary(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val workoutCount: Int,
    val trainingTime: Duration,
    val workingSetCount: Int,
    val totalReps: Int,
    val performance: ExercisePerformanceChange,
    val musclesTrained: List<TrainedMuscle>,
)

data class ExercisePerformanceChange(
    val improved: Int,
    val maintained: Int,
    val declined: Int,
) {
    val comparableCount: Int get() = improved + maintained + declined
}

fun List<WorkoutSession>.lastSevenDaysSummary(today: LocalDate): LastSevenDaysSummary {
    val startDate = today.minusDays(6)
    val completed = filter { it.state is WorkoutState.Completed }
    val inWindow = completed.filter { session ->
        session.startedAt.toLocalDate() in startDate..today
    }
    val beforeWindow = completed.filter { session ->
        session.startedAt.toLocalDate() < startDate
    }
    return LastSevenDaysSummary(
        startDate = startDate,
        endDate = today,
        workoutCount = inWindow.size,
        trainingTime = inWindow.totalTrainingTime(),
        workingSetCount = inWindow.sumOf { session ->
            session.exercises.sumOf { it.sets.size }
        },
        totalReps = inWindow.sumOf { session ->
            session.exercises.sumOf { exercise -> exercise.sets.sumOf { it.reps } }
        },
        performance = performanceChange(inWindow, beforeWindow),
        musclesTrained = inWindow.musclesTrained(),
    )
}

private fun List<WorkoutSession>.totalTrainingTime(): Duration =
    fold(Duration.ZERO) { total, session ->
        total + (session.duration ?: Duration.ZERO)
    }

private fun performanceChange(
    inWindow: List<WorkoutSession>,
    beforeWindow: List<WorkoutSession>,
): ExercisePerformanceChange {
    var improved = 0
    var maintained = 0
    var declined = 0
    inWindow.exerciseIds().forEach { exerciseId ->
        val current = inWindow.bestTopSetOneRm(exerciseId) ?: return@forEach
        val previous = beforeWindow.latestTopSetOneRm(exerciseId) ?: return@forEach
        when {
            current > previous -> improved++
            current < previous -> declined++
            else -> maintained++
        }
    }
    return ExercisePerformanceChange(improved, maintained, declined)
}

private fun List<WorkoutSession>.exerciseIds(): Set<String> =
    flatMap { session -> session.exercises.map { it.id } }.toSet()

private fun List<WorkoutSession>.bestTopSetOneRm(exerciseId: String): Weight? =
    mapNotNull { session -> session.exerciseOrNull(exerciseId)?.topSet?.oneRm }.maxOrNull()

private fun List<WorkoutSession>.latestTopSetOneRm(exerciseId: String): Weight? =
    filter { session -> session.exerciseOrNull(exerciseId) != null }
        .maxByOrNull { it.startedAt }
        ?.exerciseOrNull(exerciseId)
        ?.topSet
        ?.oneRm

private fun WorkoutSession.exerciseOrNull(exerciseId: String): WorkoutExercise? =
    exercises.firstOrNull { it.id == exerciseId }

private fun List<WorkoutSession>.musclesTrained(): List<TrainedMuscle> =
    flatMap { session -> session.exercises.flatMap { it.exerciseDefinition.trainedMuscles() } }
        .toSet()
        .sortedBy { it.ordinal }
