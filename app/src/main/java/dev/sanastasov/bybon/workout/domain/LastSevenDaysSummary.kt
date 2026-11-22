package dev.sanastasov.bybon.workout.domain

import java.time.LocalDate
import kotlin.time.Duration

data class LastSevenDaysSummary(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val workoutCount: Int,
    val trainingTime: Duration,
    val workingSetCount: Int,
    val volumeLoad: Weight?,
    val performance: ExercisePerformanceChange,
    val daysTrained: DaysTrained,
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
        volumeLoad = inWindow
            .flatMap { it.exercises }
            .flatMap { it.sets }
            .volumeLoad(),
        performance = performanceChange(inWindow, beforeWindow),
        daysTrained = daysTrained(
            today,
            completed.map { it.startedAt.toLocalDate() }.toSet(),
        ),
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
        val current = inWindow.bestPerformanceSet(exerciseId) ?: return@forEach
        val previous = beforeWindow.latestPerformanceSet(exerciseId) ?: return@forEach
        when (current.performanceChange(previous)) {
            is OneRmChange.Gain -> improved++
            is OneRmChange.Loss -> declined++
            OneRmChange.Unchanged -> maintained++
            null -> Unit
        }
    }
    return ExercisePerformanceChange(improved, maintained, declined)
}

private fun List<WorkoutSession>.exerciseIds(): Set<String> =
    flatMap { session -> session.exercises.map { it.id } }.toSet()

private fun List<WorkoutSession>.bestPerformanceSet(exerciseId: String): ExerciseSet? =
    mapNotNull { session -> session.exerciseOrNull(exerciseId)?.performanceTopSet() }
        .maxWithOrNull(performanceSetOrder)

private fun List<WorkoutSession>.latestPerformanceSet(exerciseId: String): ExerciseSet? =
    filter { session -> session.exerciseOrNull(exerciseId) != null }
        .maxByOrNull { it.startedAt }
        ?.exerciseOrNull(exerciseId)
        ?.performanceTopSet()

private fun WorkoutSession.exerciseOrNull(exerciseId: String): WorkoutExercise? =
    exercises.firstOrNull { it.id == exerciseId }
