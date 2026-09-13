package dev.sanastasov.bybon.workout.data

import dev.sanastasov.bybon.workout.domain.ExerciseSet
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.exercisesMap
import dev.sanastasov.bybon.workout.domain.fullBodyB
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.seconds

/** Last Full Body B from strong-backup-sample.csv (Workout #250), working sets only. */
val sampleFullBodyBCompleted = WorkoutSession(
    planId = fullBodyB.id,
    planName = fullBodyB.name,
    planDescription = "Friday full body workout",
    exercises = listOf(
        completedExercise(
            "rdl-bb",
            listOf(45f to 12, 45f to 12)
        ),
        completedExercise(
            "incline-bench-press-db",
            listOf(20f to 13, 20f to 11, 20f to 8)
        ),
        completedExercise(
            "split-squat-db",
            listOf(14f to 10, 14f to 10)
        ),
        completedExercise(
            "incline-row-db",
            listOf(22f to 14, 22f to 12, 22f to 10)
        )
    ),
    state = WorkoutState.Completed(
        startedAt = LocalDateTime.of(2026, 8, 13, 18, 25, 54),
        duration = 2864.seconds
    )
)

private fun completedExercise(
    exerciseId: String,
    weightAndReps: List<Pair<Float, Int>>
): WorkoutExercise {
    val definition = exercisesMap[exerciseId]!!
    val reps = weightAndReps.map { it.second }
    return WorkoutExercise(
        exerciseDefinition = definition,
        repRange = reps.min()..reps.max(),
        sets = weightAndReps.map { (kg, reps) ->
            ExerciseSet(
                exerciseDefinition = definition,
                weight = Weight.kilograms(kg),
                reps = reps,
                setState = SetState.Completed
            )
        }
    )
}
