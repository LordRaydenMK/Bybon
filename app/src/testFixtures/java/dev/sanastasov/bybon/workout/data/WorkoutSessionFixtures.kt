package dev.sanastasov.bybon.workout.data

import dev.sanastasov.bybon.workout.domain.ExerciseSet
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.exercisesMap
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.minutes

fun completedSession(
    planId: String,
    planName: String,
    startedAt: LocalDateTime,
    exercises: List<WorkoutExercise>,
): WorkoutSession = WorkoutSession(
    planId = WorkoutPlanId(planId),
    planName = planName,
    planDescription = null,
    exercises = exercises,
    state = WorkoutState.Completed(startedAt, 40.minutes),
)

fun completedExercise(
    exerciseId: String,
    vararg weightAndReps: Pair<Float, Int>,
): WorkoutExercise {
    val definition = exercisesMap.getValue(exerciseId)
    return WorkoutExercise(
        exerciseDefinition = definition,
        repRange = 8..12,
        sets = weightAndReps.map { (kg, reps) ->
            ExerciseSet(
                exerciseDefinition = definition,
                weight = Weight.kilograms(kg),
                reps = reps,
                setState = SetState.Completed,
            )
        },
    )
}
