@file:Suppress("TooManyFunctions")

package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.Duration

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
                planedExercise.exercise,
                planedExercise.repRange,
                (1..planedExercise.sets).map { setNumber ->
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
                planedExercise.warmupSets.mapIndexed { warmupIndex, plannedWarmup ->
                    val previousWarmup = previousExercise?.warmupSets?.getOrNull(warmupIndex)
                    ExerciseSet(
                        planedExercise.exercise,
                        previousWarmup?.weight ?: plannedWarmup.weight,
                        previousWarmup?.reps ?: plannedWarmup.reps,
                        SetState.NotStated,
                        previous = previousWarmup?.let {
                            PreviousSetPerformance(it.weight, it.reps)
                        },
                    )
                },
            )
        },
        startedAt,
    )
    return session.startWorkout()
}

@JvmInline
value class Weight(
    private val value: Int,
) : Comparable<Weight> {

    val kilograms: String
        get() = "%.2f".format(Locale.US, kilogramsValue).trimEnd('0').trimEnd('.')

    val kilogramsValue: Float
        get() = value / 100f

    operator fun plus(other: Weight): Weight = Weight(value + other.value)

    operator fun minus(other: Weight): Weight = Weight((value - other.value).coerceAtLeast(0))

    override fun compareTo(other: Weight): Int = value.compareTo(other.value)

    companion object {

        fun kilograms(value: Int): Weight = Weight(value * 100)

        fun kilograms(value: Float): Weight = Weight((value * 100).roundToInt())

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
    val oneRm: Weight?
        get() = oneRmOrNull(weight, reps)
}

data class ExerciseSet(
    val exerciseDefinition: ExerciseDefinition,
    val weight: Weight,
    val reps: Int,
    val setState: SetState,
    val previous: PreviousSetPerformance? = null,
) {
    val oneRm: Weight?
        get() = oneRmOrNull(weight, reps)
}

private fun oneRmOrNull(weight: Weight, reps: Int): Weight? {
    val kg = weight.kilogramsValue
    if (kg <= 0f || reps <= 0) return null
    return Weight.kilograms(estimateOneRmKg(kg, reps))
}

data class WorkoutExercise(
    val exerciseDefinition: ExerciseDefinition,
    val repRange: IntRange,
    val sets: List<ExerciseSet>,
    val warmupSets: List<ExerciseSet> = emptyList(),
) {
    val id: String = exerciseDefinition.id

    val orderedSets: List<ExerciseSet>
        get() = warmupSets + sets

    val canRemoveSet: Boolean
        get() {
            val last = orderedSets.lastOrNull() ?: return false
            return last.setState != SetState.Completed
        }
}

sealed class WorkoutState {
    data object NotStarted : WorkoutState()
    data object InProgress : WorkoutState()
    data class Completed(
        val duration: Duration,
    ) : WorkoutState()
}

data class WorkoutSession(
    val planId: WorkoutPlanId,
    val planName: String,
    val planDescription: String?,
    val exercises: List<WorkoutExercise>,
    val startedAt: LocalDateTime,
    val state: WorkoutState = WorkoutState.NotStarted,
) {
    val id: WorkoutSessionId
        get() = WorkoutSessionId(planId, startedAt)

    val workoutSets: List<ExerciseSet> = exercises.flatMap { it.orderedSets }

    init {
        require(workoutSets.map { it.setState }.filter { it == SetState.InProgress }.size <= 1) {
            "At most 1 set can be in progress. Found ${workoutSets.filter {
                it.setState == SetState.InProgress
            }}"
        }
        if (state is WorkoutState.Completed) {
            val incomplete = workoutSets.filter { it.setState != SetState.Completed }
            require(workoutSets.isNotEmpty() && incomplete.isEmpty()) {
                "Completed workout $id has incomplete sets: $incomplete"
            }
        }
    }
}

fun WorkoutSession.completeSet(
    exercise: WorkoutExercise,
    setIndex: Int,
    isWarmup: Boolean = false,
): WorkoutSession {
    val next = nextSetAfter(exercise.id, setIndex, isWarmup)
    val updated = updateExerciseSet(exercise, setIndex, isWarmup) {
        it.copy(setState = SetState.Completed)
    }
    return if (next != null) {
        val nextExercise = updated.exercises.first { it.id == next.exerciseId }
        updated.updateExerciseSet(nextExercise, next.index, next.isWarmup) {
            it.copy(setState = SetState.InProgress)
        }
    } else {
        updated
    }
}

fun WorkoutSession.addSet(exercise: WorkoutExercise): WorkoutSession =
    updateExercise(exercise.id) { current ->
        val lastWorkSet = current.sets.lastOrNull()
        val template =
            lastWorkSet ?: current.warmupSets.lastOrNull() ?: return@updateExercise current
        val newSetState = if (lastWorkSet?.setState == SetState.Completed ||
            (lastWorkSet == null && template.setState == SetState.Completed)
        ) {
            SetState.InProgress
        } else {
            SetState.NotStated
        }
        current.copy(
            sets = current.sets + template.copy(setState = newSetState, previous = null),
        )
    }

@Suppress("ReturnCount")
fun WorkoutSession.removeLastSet(exercise: WorkoutExercise): WorkoutSession {
    val hadInProgress = workoutSets.any { it.setState == SetState.InProgress }
    val updated = updateExercise(exercise.id) { current ->
        when {
            current.sets.lastOrNull()?.let { it.setState != SetState.Completed } == true ->
                current.copy(sets = current.sets.dropLast(1))

            current.warmupSets.lastOrNull()?.let { it.setState != SetState.Completed } == true ->
                current.copy(warmupSets = current.warmupSets.dropLast(1))

            else -> current
        }
    }
    if (!hadInProgress || updated.workoutSets.any { it.setState == SetState.InProgress }) {
        return updated
    }
    val next = updated.firstNotStartedSet() ?: return updated
    val nextExercise = updated.exercises.first { it.id == next.exerciseId }
    return updated.updateExerciseSet(nextExercise, next.index, next.isWarmup) {
        it.copy(setState = SetState.InProgress)
    }
}

fun WorkoutSession.convertFirstWorkSetToWarmup(exercise: WorkoutExercise): WorkoutSession =
    updateExercise(exercise.id) { current ->
        val firstWorkSet = current.sets.firstOrNull() ?: return@updateExercise current
        current.copy(
            warmupSets = current.warmupSets + firstWorkSet,
            sets = current.sets.drop(1),
        )
    }

fun WorkoutSession.convertLastWarmupToWorkSet(exercise: WorkoutExercise): WorkoutSession =
    updateExercise(exercise.id) { current ->
        val lastWarmup = current.warmupSets.lastOrNull() ?: return@updateExercise current
        current.copy(
            warmupSets = current.warmupSets.dropLast(1),
            sets = listOf(lastWarmup) + current.sets,
        )
    }

fun WorkoutSession.updateWeight(
    exercise: WorkoutExercise,
    setIndex: Int,
    weight: Weight,
    isWarmup: Boolean = false,
): WorkoutSession = updateExerciseSet(exercise, setIndex, isWarmup) {
    it.copy(weight = weight)
}

fun WorkoutSession.updateReps(
    exercise: WorkoutExercise,
    setIndex: Int,
    count: Int,
    isWarmup: Boolean = false,
): WorkoutSession = updateExerciseSet(exercise, setIndex, isWarmup) {
    it.copy(reps = count)
}

fun WorkoutPlan.toOverviewSession(previousSession: WorkoutSession? = null): WorkoutSession =
    toWorkoutSession(previousSession).asOverviewDraft()

fun WorkoutSession.asOverviewDraft(): WorkoutSession = copy(
    state = WorkoutState.NotStarted,
    exercises = exercises.map { exercise ->
        exercise.copy(
            sets = exercise.sets.map { it.copy(setState = SetState.NotStated) },
            warmupSets = exercise.warmupSets.map { it.copy(setState = SetState.NotStated) },
        )
    },
)

@Suppress("ReturnCount")
fun WorkoutSession.startWorkout(): WorkoutSession {
    val first = firstNotStartedSet() ?: return this
    val exercise = exercises.first { it.id == first.exerciseId }
    return updateExerciseSet(exercise, first.index, first.isWarmup) {
        it.copy(setState = SetState.InProgress)
    }
}

fun WorkoutSession.adjustAll(increase: Boolean): WorkoutSession =
    copy(exercises = exercises.map { it.adjust(increase) })

fun WorkoutSession.adjustExercise(exercise: WorkoutExercise, increase: Boolean): WorkoutSession =
    updateExercise(exercise.id) { it.adjust(increase) }

fun WorkoutSession.resetTo(previous: WorkoutSession): WorkoutSession = previous

fun WorkoutSession.resetExercise(
    exercise: WorkoutExercise,
    previous: WorkoutSession,
): WorkoutSession {
    val restored = previous.exercises.firstOrNull { it.id == exercise.id } ?: return this
    return updateExercise(exercise.id) { restored }
}

private fun WorkoutExercise.adjust(increase: Boolean): WorkoutExercise {
    val originalFirst = sets.firstOrNull() ?: return this
    val increment = exerciseDefinition.equipment.weightIncrement
    val adjustedFirst = originalFirst.adjust(repRange, increment, increase)
    return copy(
        sets = listOf(adjustedFirst) + sets.drop(1).map { set ->
            set.followFirstWorkSet(originalFirst, adjustedFirst, repRange, increase)
        },
    )
}

@Suppress("ReturnCount")
internal fun ExerciseSet.adjust(
    repRange: IntRange,
    increment: Weight,
    increase: Boolean,
): ExerciseSet {
    val expandedRange = repRange.expanded()
    if (increase) {
        if (reps < repRange.last) {
            return copy(reps = reps + 1)
        }
        if (increment.kilogramsValue <= 0f) return this
        var newWeight = weight + increment
        repeat(64) {
            withWeightPreservingOneRm(
                newWeight,
                preferredRange = repRange,
                fallbackRange = expandedRange,
                increase = true,
            )?.let { return it }
            newWeight += increment
        }
        return this
    }
    if (reps > repRange.first) {
        return copy(reps = reps - 1)
    }
    if (increment.kilogramsValue <= 0f || weight.kilogramsValue <= 0f) return this
    var newWeight = weight - increment
    repeat(64) {
        if (newWeight.kilogramsValue <= 0f) return this
        withWeightPreservingOneRm(
            newWeight,
            preferredRange = repRange,
            fallbackRange = expandedRange,
            increase = false,
        )?.let { return it }
        newWeight -= increment
    }
    return this
}

private fun ExerciseSet.followFirstWorkSet(
    originalFirst: ExerciseSet,
    adjustedFirst: ExerciseSet,
    repRange: IntRange,
    increase: Boolean,
): ExerciseSet {
    val expandedRange = repRange.expanded()
    if (adjustedFirst.weight == originalFirst.weight) {
        val delta = adjustedFirst.reps - originalFirst.reps
        return copy(reps = (reps + delta).coerceIn(expandedRange))
    }
    return withWeightPreservingOneRm(
        adjustedFirst.weight,
        preferredRange = expandedRange,
        fallbackRange = expandedRange,
        increase = increase,
    ) ?: copy(weight = adjustedFirst.weight)
}

private fun ExerciseSet.withWeightPreservingOneRm(
    newWeight: Weight,
    preferredRange: IntRange,
    fallbackRange: IntRange,
    increase: Boolean,
): ExerciseSet? {
    val currentOneRm = oneRm
    fun pick(range: IntRange): Int? {
        val candidates = range.filter { candidateReps ->
            val candidateOneRm = oneRmOrNull(newWeight, candidateReps) ?: return@filter false
            if (increase) {
                currentOneRm == null || candidateOneRm > currentOneRm
            } else {
                currentOneRm == null || candidateOneRm < currentOneRm
            }
        }
        return if (increase) candidates.minOrNull() else candidates.maxOrNull()
    }
    val reps = pick(preferredRange) ?: pick(fallbackRange) ?: return null
    return copy(weight = newWeight, reps = reps)
}

private fun IntRange.expanded(amount: Int = 2): IntRange =
    (first - amount).coerceAtLeast(1)..(last + amount)

private fun WorkoutSession.updateExercise(
    exerciseId: String,
    update: (WorkoutExercise) -> WorkoutExercise,
): WorkoutSession = copy(
    exercises = exercises.map { exercise ->
    if (exercise.id == exerciseId) {
        update(exercise)
    } else {
        exercise
    }
}
)

private fun WorkoutSession.updateExerciseSet(
    exercise: WorkoutExercise,
    setIndex: Int,
    isWarmup: Boolean = false,
    update: (ExerciseSet) -> ExerciseSet,
): WorkoutSession = updateExercise(exercise.id) { current ->
    if (isWarmup) {
        current.copy(
            warmupSets = current.warmupSets.mapIndexed { index, set ->
                if (index == setIndex) update(set) else set
            },
        )
    } else {
        current.copy(
            sets = current.sets.mapIndexed { index, set ->
                if (index == setIndex) update(set) else set
            },
        )
    }
}

private data class SetRef(
    val exerciseId: String,
    val index: Int,
    val isWarmup: Boolean,
)

private fun WorkoutSession.nextSetAfter(
    exerciseId: String,
    setIndex: Int,
    isWarmup: Boolean,
): SetRef? {
    val exercise = exercises.first { it.id == exerciseId }
    if (isWarmup) {
        if (setIndex < exercise.warmupSets.lastIndex) {
            return SetRef(exerciseId, setIndex + 1, isWarmup = true)
        }
        if (exercise.sets.isNotEmpty()) {
            return SetRef(exerciseId, 0, isWarmup = false)
        }
    } else if (setIndex < exercise.sets.lastIndex) {
        return SetRef(exerciseId, setIndex + 1, isWarmup = false)
    }
    val nextExercise = exercises.getOrNull(exercises.indexOfFirst { it.id == exerciseId } + 1)
        ?: return null
    if (nextExercise.warmupSets.isNotEmpty()) {
        return SetRef(nextExercise.id, 0, isWarmup = true)
    }
    if (nextExercise.sets.isNotEmpty()) {
        return SetRef(nextExercise.id, 0, isWarmup = false)
    }
    return null
}

private fun WorkoutSession.firstNotStartedSet(): SetRef? =
    exercises.firstNotNullOfOrNull { exercise ->
        val warmupIndex = exercise.warmupSets.indexOfFirst { it.setState == SetState.NotStated }
        when {
            warmupIndex >= 0 -> SetRef(exercise.id, warmupIndex, isWarmup = true)

            else -> {
                val workIndex = exercise.sets.indexOfFirst { it.setState == SetState.NotStated }
                if (workIndex >= 0) SetRef(exercise.id, workIndex, isWarmup = false) else null
            }
        }
    }

sealed class WorkoutSessionAction {
    data class OnCompleteSet(
        val exercise: WorkoutExercise,
        val index: Int,
        val isWarmup: Boolean = false,
    ) : WorkoutSessionAction()

    data class OnWeightUpdated(
        val newWeight: String,
        val exercise: WorkoutExercise,
        val index: Int,
        val isWarmup: Boolean = false,
    ) : WorkoutSessionAction()

    data class OnRepsUpdated(
        val newReps: String,
        val exercise: WorkoutExercise,
        val index: Int,
        val isWarmup: Boolean = false,
    ) : WorkoutSessionAction()

    data class OnAddSet(
        val exercise: WorkoutExercise
    ) : WorkoutSessionAction()

    data class RemoveLastSet(
        val exercise: WorkoutExercise
    ) : WorkoutSessionAction()

    data class OnConvertToWarmup(
        val exercise: WorkoutExercise
    ) : WorkoutSessionAction()

    data class OnConvertToWorkSet(
        val exercise: WorkoutExercise
    ) : WorkoutSessionAction()
}
