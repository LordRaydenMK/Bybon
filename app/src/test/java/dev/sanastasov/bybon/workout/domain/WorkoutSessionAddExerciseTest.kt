package dev.sanastasov.bybon.workout.domain

import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.minutes
import org.junit.Test

class WorkoutSessionAddExerciseTest {

    @Test
    fun `addExercise appends a session exercise with defaults`() {
        val session = fullBodyA.toOverviewSession()
        val curl = catalogExercise("incline-curl-db")

        val actual = session.addExercise(curl)
        val added = actual.exercises.last()

        assert(added.exerciseDefinition == curl)
        assert(added.sets.size == 3)
        assert(added.warmupSets == null)
        assert(added.repRange == 8..12)
        assert(added.restAfterWorkSet == 1.minutes)
        assert(added.sets.all { it.weight == Weight.kilograms(50) })
        assert(added.sets.all { it.reps == 8 })
        assert(added.sets.all { it.setState == SetState.NotStated })
        assert(actual.exercises.dropLast(1) == session.exercises)
    }

    @Test
    fun `addExercise throws when the exercise is already in the session`() {
        val session = fullBodyA.toOverviewSession()
        val bench = catalogExercise("bench-press-bb")
        val error = assertFailsWith<IllegalStateException> {
            session.addExercise(bench)
        }
        assert(error.message == "Exercise bench-press-bb is already in the session")
    }

    @Test
    fun `addExerciseIfAbsent keeps the session when the exercise is already present`() {
        val session = fullBodyA.toOverviewSession()
        val bench = catalogExercise("bench-press-bb")

        assert(session.addExerciseIfAbsent(bench) == session)
    }

    @Test
    fun `addExercise does not start the new exercise`() {
        val session = fullBodyA.toWorkoutSession()
        val curl = catalogExercise("incline-curl-db")

        val actual = session.addExercise(curl)

        assert(actual.workoutSets.any { it.setState == SetState.InProgress })
        assert(actual.exercises.last().orderedSets.all { it.setState == SetState.NotStated })
        assert(
            actual.exercises.first().warmupSets!!.first().setState == SetState.InProgress,
        )
    }

    @Test
    fun `removeExercise drops the matching exercise`() {
        val session = fullBodyA.toOverviewSession()

        val actual = session.removeExercise("leg-curl")

        assert(actual.exercises.none { it.id == "leg-curl" })
        assert(actual.exercises.size == session.exercises.size - 1)
        assert(actual.exercises.first() == session.exercises.first())
    }

    @Test
    fun `removeExercise throws when the exercise is not in the session`() {
        val session = fullBodyA.toOverviewSession()
        val error = assertFailsWith<IllegalStateException> {
            session.removeExercise("missing")
        }
        assert(error.message == "Exercise missing is not in the session")
    }

    @Test
    fun `removeExercise throws when only one exercise remains`() {
        val session = fullBodyA.toOverviewSession().let { draft ->
            draft.copy(exercises = listOf(draft.exercises.first()))
        }
        val error = assertFailsWith<IllegalStateException> {
            session.removeExercise("bench-press-bb")
        }
        assert(error.message == "Cannot remove last exercise from the session")
    }

    @Test
    fun `removeExercise keeps in-progress on another exercise`() {
        val session = fullBodyA.toWorkoutSession()
        val actual = session.removeExercise("leg-curl")

        assert(actual.exercises.none { it.id == "leg-curl" })
        assert(
            actual.exercises.first().warmupSets!!.first().setState == SetState.InProgress,
        )
    }

    @Test
    fun `removeExercise starts the next exercise when the in-progress one is removed`() {
        val session = fullBodyA.toWorkoutSession()

        val actual = session.removeExercise("bench-press-bb")

        assert(actual.exercises.none { it.id == "bench-press-bb" })
        assert(actual.exercises.first().id == "squat-bb")
        assert(
            actual.exercises.first().warmupSets!!.first().setState == SetState.InProgress,
        )
    }

    @Test
    fun `removeExercise throws when the exercise is completed`() {
        val session = twoExerciseSession(
            firstState = SetState.Completed,
            secondState = SetState.InProgress,
        )

        val error = assertFailsWith<IllegalStateException> {
            session.removeExercise("bench-press-bb")
        }
        assert(error.message == "Cannot remove completed exercise bench-press-bb")
    }

    @Test
    fun `removeExercise completes the session when remaining exercises are done`() {
        val session = twoExerciseSession(
            firstState = SetState.Completed,
            secondState = SetState.InProgress,
        )

        val actual = session.removeExercise("incline-curl-db")

        assert(actual.exercises.single().id == "bench-press-bb")
        assert(actual.state is WorkoutState.Completed)
        assert(actual.duration != null)
    }

    @Test
    fun `toWorkoutSession follows the plan even if the previous session added or removed exercises`() {
        val previous = fullBodyA.toWorkoutSession()
            .removeExercise("leg-curl")
            .addExercise(catalogExercise("incline-curl-db"))
            .let { session ->
                session.copy(
                    exercises = session.exercises.map { exercise ->
                        exercise.copy(
                            warmupSets = exercise.warmupSets?.map {
                                it.copy(setState = SetState.Completed)
                            },
                            sets = exercise.sets.map { it.copy(setState = SetState.Completed) },
                        )
                    },
                    duration = kotlin.time.Duration.ZERO,
                    startedAt = java.time.LocalDateTime.of(2026, 1, 1, 12, 0),
                )
            }

        val actual = fullBodyA.toWorkoutSession(previous)

        assert(actual.exercises.map { it.id } == fullBodyA.sets.map { it.exercise.id })
        assert(actual.exercises.none { it.id == "incline-curl-db" })
        assert(actual.exercises.any { it.id == "leg-curl" })
    }

    @Test
    fun `canRemoveExercise is false for completed or last remaining exercises`() {
        val session = twoExerciseSession(
            firstState = SetState.Completed,
            secondState = SetState.InProgress,
        )
        val completed = session.exercises.first()
        val inProgress = session.exercises.last()

        assert(!session.canRemoveExercise(completed))
        assert(session.canRemoveExercise(inProgress))
        assert(!session.removeExercise(inProgress.id).canRemoveExercise(session.exercises.first()))
    }
}

private fun twoExerciseSession(firstState: SetState, secondState: SetState): WorkoutSession {
    val bench = catalogExercise("bench-press-bb")
    val curl = catalogExercise("incline-curl-db")
    return WorkoutSession(
        planId = fullBodyA.id,
        planName = fullBodyA.name,
        planDescription = null,
        exercises = listOf(
            WorkoutExercise(
                exerciseDefinition = bench,
                repRange = 8..10,
                sets = listOf(ExerciseSet(bench, Weight.kilograms(50), 8, firstState)),
            ),
            WorkoutExercise(
                exerciseDefinition = curl,
                repRange = 8..12,
                sets = listOf(ExerciseSet(curl, Weight.kilograms(12), 8, secondState)),
            ),
        ),
        startedAt = java.time.LocalDateTime.of(2026, 1, 1, 12, 0),
    )
}
