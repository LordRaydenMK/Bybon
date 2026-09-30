package dev.sanastasov.bybon.workout.domain

import kotlin.test.assertFailsWith
import org.junit.Test

class WorkoutSessionReorderTest {

    @Test
    fun `moveExerciseDown swaps with the next exercise`() {
        val session = fullBodyA.toOverviewSession()

        val actual = session.moveExerciseDown("bench-press")

        assert(actual.exercises.map { it.id }.take(2) == listOf("squat", "bench-press"))
        assert(actual.exercises.drop(2) == session.exercises.drop(2))
    }

    @Test
    fun `moveExerciseUp swaps with the previous exercise`() {
        val session = fullBodyA.toOverviewSession()

        val actual = session.moveExerciseUp("squat")

        assert(actual.exercises.map { it.id }.take(2) == listOf("squat", "bench-press"))
        assert(actual.exercises.drop(2) == session.exercises.drop(2))
    }

    @Test
    fun `moveExerciseUp throws for the first exercise`() {
        val session = fullBodyA.toOverviewSession()
        val error = assertFailsWith<IllegalStateException> {
            session.moveExerciseUp("bench-press")
        }
        assert(error.message == "Cannot move bench-press up")
    }

    @Test
    fun `moveExerciseDown throws for the last exercise`() {
        val session = fullBodyA.toOverviewSession()
        val lastId = session.exercises.last().id
        val error = assertFailsWith<IllegalStateException> {
            session.moveExerciseDown(lastId)
        }
        assert(error.message == "Cannot move $lastId down")
    }

    @Test
    fun `unknown exercise id throws when moving`() {
        val session = fullBodyA.toOverviewSession()
        val error = assertFailsWith<IllegalStateException> {
            session.moveExerciseUp("missing")
        }
        assert(error.message == "Exercise missing is not in the session")
        assertFailsWith<IllegalStateException> { session.moveExerciseDown("missing") }
    }

    @Test
    fun `startWorkout after reorder marks the new first exercise in progress`() {
        val overview = fullBodyA.toOverviewSession().moveExerciseDown("bench-press")

        val actual = overview.startWorkout()
        val squat = actual.exercises.first()

        assert(squat.id == "squat")
        assert(checkNotNull(squat.warmupSets).first().setState == SetState.InProgress)
        assert(
            actual.exercises.first { it.id == "bench-press" }
                .warmupSets!!
                .all { it.setState == SetState.NotStated },
        )
        assert(actual.state == WorkoutState.InProgress)
    }
}
