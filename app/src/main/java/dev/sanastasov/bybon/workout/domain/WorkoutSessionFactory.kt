package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime

private val defaultWarmupReps = listOf(8, 4, 3)

fun WorkoutPlan.toWorkoutSession(
    previousSession: WorkoutSession? = null,
    startedAt: LocalDateTime = LocalDateTime.now(),
): WorkoutSession {
    val session = WorkoutSession(
        id,
        name,
        description,
        sets.map { planedExercise ->
            val previousExercise = previousSession?.exercises?.firstOrNull {
                it.id == planedExercise.exercise.id
            }
            WorkoutExercise(
                exerciseDefinition = planedExercise.exercise,
                repRange = planedExercise.repRange,
                warmupSets = warmupSetsFromPlan(
                    planedExercise.exercise,
                    planedExercise.warmupSets,
                    previousExercise?.warmupSets,
                ),
                sets = (1..planedExercise.sets).map { setNumber ->
                    val setIndex = setNumber - 1
                    val previousSet = previousExercise?.sets?.getOrNull(setIndex)
                    ExerciseSet(
                        planedExercise.exercise,
                        previousSet?.weight ?: Weight.kilograms(50),
                        previousSet?.reps ?: planedExercise.repRange.first,
                        SetState.NotStated,
                        previous = previousSet?.let {
                            PreviousSetPerformance(it.weight, it.reps)
                        },
                    )
                },
                restAfterWorkSet = previousExercise?.restAfterWorkSet
                    ?: planedExercise.restAfterWorkSet,
                note = planedExercise.note,
            )
        },
        startedAt,
    )
    return session.startWorkout()
}

private fun warmupSetsFromPlan(
    exercise: ExerciseDefinition,
    count: Int,
    previousWarmups: List<ExerciseSet>?,
): List<ExerciseSet>? {
    require(count >= 0) { "warmupSets must be >= 0" }
    if (count == 0) return null
    return List(count) { index ->
        val previousWarmup = previousWarmups?.getOrNull(index)
        ExerciseSet(
            exerciseDefinition = exercise,
            weight = previousWarmup?.weight ?: exercise.equipment.defaultWarmupWeight,
            reps = previousWarmup?.reps
                ?: defaultWarmupReps.getOrElse(index) { defaultWarmupReps.last() },
            setState = SetState.NotStated,
            previous = previousWarmup?.let {
                PreviousSetPerformance(it.weight, it.reps)
            },
        )
    }
}

fun WorkoutPlan.toOverviewSession(previousSession: WorkoutSession? = null): WorkoutSession =
    toWorkoutSession(previousSession).asOverviewDraft()

fun WorkoutSession.asOverviewDraft(): WorkoutSession = copy(
    state = WorkoutState.NotStarted,
    exercises = exercises.map { exercise ->
        exercise.copy(
            sets = exercise.sets.map { it.copy(setState = SetState.NotStated) },
            warmupSets = exercise.warmupSets?.map { it.copy(setState = SetState.NotStated) },
        )
    },
)

fun WorkoutSession.startWorkout(): WorkoutSession {
    val first = firstNotStartedSet() ?: return this
    val exercise = exercises.first { it.id == first.exerciseId }
    return updateExerciseSet(exercise, first.index, first.isWarmup) {
        it.copy(setState = SetState.InProgress)
    }
}
