package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime
import java.time.Period

data class ExercisePerformance(
    val personalRecordOneRm: Weight?,
    val recentSessions: List<ExerciseSessionLog>,
)

data class ExerciseSessionLog(
    val startedAt: LocalDateTime,
    val planName: String,
    val topSetOneRm: Weight?,
    val workSets: List<ExerciseSet>,
)

fun List<WorkoutSession>.exercisePerformance(
    exerciseId: String,
    now: LocalDateTime,
    historyWindow: Period = Period.ofMonths(3),
): ExercisePerformance {
    require(all { it.state is WorkoutState.Completed }) {
        "exercisePerformance requires completed sessions"
    }
    val matching = mapNotNull { session ->
        val exercise = session.exercises.firstOrNull { candidate ->
            candidate.id == exerciseId
        } ?: return@mapNotNull null
        session to exercise
    }
    val personalRecordOneRm = matching
        .flatMap { it.second.sets }
        .mapNotNull { it.oneRm }
        .maxOrNull()
    val windowStart = now.minus(historyWindow)
    val recentSessions = matching
        .filter { (session, _) ->
            !session.startedAt.isBefore(windowStart) && !session.startedAt.isAfter(now)
        }
        .sortedByDescending { it.first.startedAt }
        .map { (session, exercise) ->
            ExerciseSessionLog(
                startedAt = session.startedAt,
                planName = session.planName,
                topSetOneRm = exercise.topSet.oneRm,
                workSets = exercise.sets,
            )
        }
    return ExercisePerformance(personalRecordOneRm, recentSessions)
}
