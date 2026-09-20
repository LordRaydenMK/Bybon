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
}
