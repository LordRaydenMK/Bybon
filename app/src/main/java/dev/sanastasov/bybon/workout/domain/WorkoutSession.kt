package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime
import kotlin.math.roundToInt
import kotlin.time.Duration

fun WorkoutPlan.toWorkoutSession(
    previousSession: WorkoutSession? = null,
): WorkoutSession =
    WorkoutSession(
        id,
        name,
        description,
        sets.mapIndexed { index, planedExercise ->
            val previousExercise = previousSession?.exercises?.firstOrNull {
                it.id == planedExercise.exercise.id
            }
            WorkoutExercise(
                planedExercise.exercise,
                planedExercise.repRange,
                (1..planedExercise.sets).map { setNumber ->
                    val setIndex = setNumber - 1
                    val previousSet = previousExercise?.sets?.getOrNull(setIndex)
                    ExerciseSet(
                        planedExercise.exercise,
                        Weight.kilograms(50),
                        planedExercise.repRange.first,
                        if (index == 0 && setNumber == 1) SetState.InProgress else SetState.NotStated,
                        previous = previousSet?.let {
                            PreviousSetPerformance(it.weight, it.reps)
                        },
                    )
                }
            )
        }
    )

@JvmInline
value class Weight(private val value: Int) {

    val kilograms: String
        get() = if (value % 10 == 0) {
            (value / 10).toString()
        } else {
            (value / 10f).toString()
        }

    val kilogramsValue: Float
        get() = value / 10f

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

/** Brzycki when reps &lt; 10, otherwise Epley. */
fun estimateOneRmKg(weightKg: Float, reps: Int): Float = if (reps < 10) {
    weightKg * 36f / (37 - reps)
} else {
    weightKg * (1 + reps / 30f)
}

data class PreviousSetPerformance(
    val weight: Weight,
    val reps: Int,
) {
    val oneRm: Float?
        get() = oneRmOrNull(weight, reps)
}

data class ExerciseSet(
    val exerciseDefinition: ExerciseDefinition,
    val weight: Weight,
    val reps: Int,
    val setState: SetState,
    val previous: PreviousSetPerformance? = null,
) {
    val oneRm: Float?
        get() = oneRmOrNull(weight, reps)
}

private fun oneRmOrNull(weight: Weight, reps: Int): Float? {
    val kg = weight.kilogramsValue
    if (kg <= 0f || reps <= 0) return null
    return estimateOneRmKg(kg, reps)
}

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

    init {
        require(workoutSets.map { it.setState }.filter { it == SetState.InProgress }.size <= 1) {
            "At most 1 set can be in progress. Found ${workoutSets.filter { it.setState == SetState.InProgress }}"
        }
    }
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
            sets = exercise.sets + lastSet.copy(setState = newSetState, previous = null)
        )
    }

fun WorkoutSession.removeLastSet(exercise: WorkoutExercise): WorkoutSession {
    val updated = updateExercise(exercise.id) { exercise ->
        exercise.copy(sets = exercise.sets.dropLast(1))
    }
    if (updated.workoutSets.any { it.setState == SetState.InProgress }) {
        return updated
    }
    val next = updated.exercises.firstNotNullOfOrNull { ex ->
        val index = ex.sets.indexOfFirst { it.setState == SetState.NotStated }
        if (index >= 0) ex to index else null
    } ?: return updated
    return updated.updateExerciseSet(next.first, next.second) {
        it.copy(setState = SetState.InProgress)
    }
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
