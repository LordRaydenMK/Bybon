package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime

private val defaultWarmupReps = listOf(8, 4, 3)

fun WorkoutPlan.toWorkoutSession(
    previousSession: WorkoutSession? = null,
    startedAt: LocalDateTime = LocalDateTime.now(),
    previousHistory: List<WorkoutSession> = emptyList(),
): WorkoutSession {
    val history = previousHistory.ifEmpty { listOfNotNull(previousSession) }
    val session = WorkoutSession(
        id,
        name,
        description,
        sets.map { planedExercise ->
            val previousExercise = history.latestCompletedExercise(planedExercise.exercise.id)
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
                        if (previousSet != null) {
                            previousSet.weight
                        } else {
                            planedExercise.exercise.equipment?.defaultWorkWeight
                        },
                        previousSet?.reps ?: planedExercise.repRange.first,
                        SetState.NotStated,
                    )
                },
                restAfterWorkSet = previousExercise?.restAfterWorkSet
                    ?: planedExercise.restAfterWorkSet,
                notes = planedExercise.notes,
            ).let { exercise ->
                if (previousExercise != null) {
                    exercise.withPreviousFrom(previousExercise)
                } else {
                    exercise
                }
            }
        },
        startedAt,
    )
    return session.startWorkout(now = startedAt)
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
            weight = if (previousWarmup != null) {
                previousWarmup.weight
            } else {
                exercise.equipment?.defaultWarmupWeight
            },
            reps = previousWarmup?.reps
                ?: defaultWarmupReps.getOrElse(index) { defaultWarmupReps.last() },
            setState = SetState.NotStated,
        )
    }
}

fun WorkoutPlan.toOverviewSession(
    previousSession: WorkoutSession? = null,
    previousHistory: List<WorkoutSession> = emptyList(),
): WorkoutSession = toWorkoutSession(
    previousSession = previousSession,
    previousHistory = previousHistory,
).asOverviewDraft()

fun WorkoutSession.asOverviewDraft(): WorkoutSession = copy(
    duration = null,
    exercises = exercises.map { exercise ->
        exercise.copy(
            sets = exercise.sets.map { it.copy(setState = SetState.NotStated) },
            warmupSets = exercise.warmupSets?.map { it.copy(setState = SetState.NotStated) },
        )
    },
)

fun WorkoutSession.startWorkout(now: LocalDateTime = LocalDateTime.now()): WorkoutSession {
    val first = takeUnless {
        it.workoutSets.any { set -> set.setState == SetState.InProgress }
    }?.firstNotStartedSet()
    val started = if (first == null) {
        this
    } else {
        val exercise = exercises.first { it.id == first.exerciseId }
        updateExerciseSet(exercise, first.index, first.isWarmup) {
            it.copy(setState = SetState.InProgress)
        }
    }
    return if (state is WorkoutState.NotStarted) {
        started.copy(startedAt = now)
    } else {
        started
    }
}
