package dev.sanastasov.bybon.workout.domain

import dev.sanastasov.bybon.bodyweight.BodyWeightEntry

fun WorkoutSession.withBodyWeightOnSets(): WorkoutSession = copy(
    exercises = exercises.map { exercise ->
        exercise.copy(
            warmupSets = exercise.warmupSets?.map { it.copy(bodyWeight = bodyWeight) },
            sets = exercise.sets.map { it.copy(bodyWeight = bodyWeight) },
        )
    },
)

fun WorkoutSession.resolveBodyWeight(entries: List<BodyWeightEntry>): WorkoutSession {
    val resolved = bodyWeightSnapshot
        ?: sevenDayBodyWeightAverage(entries, startedAt.toLocalDate())
        ?: bodyWeight
    return copy(bodyWeight = resolved).withBodyWeightOnSets()
}

fun WorkoutSession.freezeBodyWeight(snapshot: Weight?): WorkoutSession {
    if (bodyWeightSnapshot != null) return withBodyWeightOnSets()
    return copy(
        bodyWeightSnapshot = snapshot,
        bodyWeight = snapshot ?: bodyWeight,
    ).withBodyWeightOnSets()
}

fun List<WorkoutSession>.resolveBodyWeights(entries: List<BodyWeightEntry>): List<WorkoutSession> {
    val resolved = map { it.resolveBodyWeight(entries) }
    return resolved.withPreviousBodyWeights()
}

private fun List<WorkoutSession>.withPreviousBodyWeights(): List<WorkoutSession> = map { session ->
    session.copy(
        exercises = session.exercises.map { exercise ->
            val previousBodyWeight = latestCompletedSessionBodyWeight(
                exercise.id,
                before = session.startedAt,
            )
            exercise.copy(
                warmupSets = exercise.warmupSets?.map { set ->
                    set.copy(
                        previous = set.previous?.copy(
                            bodyWeight = previousBodyWeight ?: set.previous.bodyWeight,
                        ),
                    )
                },
                sets = exercise.sets.map { set ->
                    set.copy(
                        previous = set.previous?.copy(
                            bodyWeight = previousBodyWeight ?: set.previous.bodyWeight,
                        ),
                    )
                },
            )
        },
    )
}

private fun List<WorkoutSession>.latestCompletedSessionBodyWeight(
    exerciseId: String,
    before: java.time.LocalDateTime,
): Weight? = mapNotNull { session ->
    if (session.state !is WorkoutState.Completed) {
        null
    } else if (!session.startedAt.isBefore(before)) {
        null
    } else if (session.exercises.none { it.id == exerciseId }) {
        null
    } else {
        session.startedAt to session.bodyWeight
    }
}.maxByOrNull { it.first }?.second
