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
}
