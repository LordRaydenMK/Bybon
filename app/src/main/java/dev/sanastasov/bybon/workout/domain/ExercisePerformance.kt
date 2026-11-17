package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime
import java.time.Period

data class ExercisePerformance(
    val personalRecord: PersonalRecord?,
    val recentSessions: List<ExerciseSessionLog>,
) {
    val personalRecordOneRm: Weight? get() = personalRecord?.oneRm
}

data class PersonalRecord(
    val set: ExerciseSet,
    val achievedAt: LocalDateTime,
) {
    val oneRm: Weight get() = checkNotNull(set.oneRm) { "PR set must have a 1RM" }
}

data class ExerciseSessionLog(
    val startedAt: LocalDateTime,
    val planName: String,
    val topSet: ExerciseSet,
    val workSets: List<ExerciseSet>,
) {
    val topSetOneRm: Weight? get() = topSet.oneRm

    val averageWorkSetOneRm: Weight?
        get() {
            val estimates = workSets.mapNotNull { it.oneRm }
            if (estimates.isEmpty()) return null
            val averageKg = estimates.map { it.kilogramsValue.toDouble() }.average()
            return Weight.kilograms(averageKg.toFloat())
        }
}

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
    val personalRecord = matching.personalRecord()
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
                topSet = exercise.topSet,
                workSets = exercise.sets,
            )
        }
    return ExercisePerformance(personalRecord, recentSessions)
}

private fun List<Pair<WorkoutSession, WorkoutExercise>>.personalRecord(): PersonalRecord? {
    val oneRm = flatMap { it.second.sets }.mapNotNull { it.oneRm }.maxOrNull() ?: return null
    val (session, exercise) = filter { (_, candidate) ->
        candidate.sets.any { set -> set.oneRm == oneRm }
    }.minBy { it.first.startedAt }
    return PersonalRecord(
        set = exercise.sets.first { it.oneRm == oneRm },
        achievedAt = session.startedAt,
    )
}
