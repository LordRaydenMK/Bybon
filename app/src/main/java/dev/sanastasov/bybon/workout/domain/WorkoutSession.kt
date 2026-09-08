package dev.sanastasov.bybon.workout.domain

import java.text.DecimalFormat
import java.time.LocalDateTime
import kotlin.math.roundToInt
import kotlin.time.Duration

fun WorkoutPlan.toWorkoutSession(): WorkoutSession =
    WorkoutSession(
        id,
        name,
        description,
        sets.mapIndexed { index, planedExercise ->
            WorkoutExercise(
                planedExercise.exercise,
                planedExercise.repRange,
                (1..planedExercise.sets).map {
                    ExerciseSet(
                        planedExercise.exercise,
                        Weight.kilograms(50),
                        planedExercise.repRange.first,
                        if (index == 0 && it == 1) SetState.InProgress else SetState.NotStated
                    )
                }
            )
        }
    )

private val DF = DecimalFormat().apply {

}

@JvmInline
value class Weight(private val value: Int) {

    val kilograms: String
        get() = (value / 10).toString()

    companion object {

        fun kilograms(value: Int): Weight = Weight(value * 10)

        fun kilograms(value: Float): Weight = Weight((value * 10).roundToInt())
    }
}

enum class SetState {
    NotStated,
    InProgress,
    Completed,
}

data class ExerciseSet(
    val exerciseDefinition: ExerciseDefinition,
    val weight: Weight,
    val reps: Int,
    val setState: SetState,
)

data class WorkoutExercise(
    val exerciseDefinition: ExerciseDefinition,
    val repRange: IntRange,
    val sets: List<ExerciseSet>,
)

sealed class WorkoutState {
    data object NotStarted : WorkoutState()
    data class InProgress(val startedAt: LocalDateTime) : WorkoutState()
    data class Completed(val startedAt: LocalDateTime, val duration: Duration) : WorkoutState()
}

data class WorkoutSession(
    val planId: WorkoutPlanId,
    val planName: String,
    val planDescription: String?,
    val exercises: List<WorkoutExercise>,
    val state: WorkoutState = WorkoutState.NotStarted,
)

fun WorkoutSession.completeSet(): WorkoutSession {
    val exerciseSets = exercises.flatMap { it.sets }
    val inProgressIndex = exerciseSets.indexOfLast { it.setState == SetState.InProgress }

    val updated = exerciseSets.mapIndexed { index, exercise ->
        when (index) {
            inProgressIndex -> exercise.copy(setState = SetState.Completed)
            inProgressIndex + 1 -> exercise.copy(setState = SetState.InProgress)
            else -> exercise
        }
    }
    val updatedSets = updated.groupBy { it.exerciseDefinition }
    val updatedExercises = exercises.map { exercise ->
        exercise.copy(sets = updatedSets[exercise.exerciseDefinition]!!)
    }
    return copy(exercises = updatedExercises)
}

sealed class WorkoutSessionAction {
    data object NoOp : WorkoutSessionAction()
    data class OnCompleteSet(val exercise: WorkoutExercise) : WorkoutSessionAction()
}
