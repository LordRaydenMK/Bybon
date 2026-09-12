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

        fun parseString(value: String): Weight = kilograms(value.toFloat())
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
) {
    val id: String = exerciseDefinition.id

    val canRemoveSet: Boolean
        get() = sets.isNotEmpty() && sets.any { it.setState == SetState.NotStated }
}

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
) {

    val workoutSets: List<ExerciseSet> = exercises.flatMap { it.sets }
}

fun WorkoutSession.completeSet(exercise: WorkoutExercise, setIndex: Int): WorkoutSession {
    val exerciseAndIndex = if (setIndex < exercise.sets.lastIndex) {
        exercise to setIndex + 1
    } else {
        if (exercises.indexOf(exercise) != exercises.lastIndex) {
            exercises[exercises.indexOf(exercise) + 1] to 0
        } else {
            null
        }
    }
    val updated = updateExerciseSet(exercise, setIndex) {
        it.copy(setState = SetState.Completed)
    }
    return if (exerciseAndIndex != null) {
        updated.updateExerciseSet(exerciseAndIndex.first, exerciseAndIndex.second) {
            it.copy(setState = SetState.InProgress)
        }
    } else {
        updated
    }
}

fun WorkoutSession.addSet(exercise: WorkoutExercise): WorkoutSession =
    updateExercise(exercise.id) { exercise ->
        val lastSet = exercise.sets.last()
        val newSetState =
            if (lastSet.setState == SetState.Completed) SetState.InProgress else SetState.NotStated
        exercise.copy(
            sets = exercise.sets + lastSet.copy(setState = newSetState)
        )
    }

fun WorkoutSession.removeLastSet(exercise: WorkoutExercise): WorkoutSession =
    updateExercise(exercise.id) { exercise ->
        exercise.copy(sets = exercise.sets.dropLast(1))
    }

fun WorkoutSession.updateWeight(
    exercise: WorkoutExercise,
    setIndex: Int,
    weight: Weight,
): WorkoutSession = updateExerciseSet(exercise, setIndex) {
    it.copy(weight = weight)
}

fun WorkoutSession.updateReps(
    exercise: WorkoutExercise,
    setIndex: Int,
    count: Int
): WorkoutSession = updateExerciseSet(exercise, setIndex) {
    it.copy(reps = count)
}

private fun WorkoutSession.updateExercise(
    exerciseId: String,
    update: (WorkoutExercise) -> WorkoutExercise
): WorkoutSession = copy(exercises = exercises.map { exercise ->
    if (exercise.id == exerciseId) {
        update(exercise)
    } else {
        exercise
    }
})

private fun WorkoutSession.updateExerciseSet(
    exercise: WorkoutExercise,
    setIndex: Int,
    update: (ExerciseSet) -> ExerciseSet
): WorkoutSession = updateExercise(exercise.id) { exercise ->
    val updated = exercise.sets.mapIndexed { index, set ->
        if (index == setIndex) {
            update(set)
        } else {
            set
        }
    }
    exercise.copy(sets = updated)
}


sealed class WorkoutSessionAction {
    data class OnCompleteSet(val exercise: WorkoutExercise, val index: Int) : WorkoutSessionAction()

    data class OnWeightUpdated(
        val newWeight: String,
        val exercise: WorkoutExercise,
        val index: Int
    ) : WorkoutSessionAction()

    data class OnRepsUpdated(
        val newReps: String,
        val exercise: WorkoutExercise,
        val index: Int
    ) : WorkoutSessionAction()

    data class OnAddSet(val exercise: WorkoutExercise) : WorkoutSessionAction()

    data class RemoveLastSet(val exercise: WorkoutExercise) : WorkoutSessionAction()
}
