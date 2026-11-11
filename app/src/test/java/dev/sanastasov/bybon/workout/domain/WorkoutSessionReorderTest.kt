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
        val error = assertFailsWith<IllegalArgumentException> {
            session.moveExerciseUp("bench-press")
        }
        assert(error.message == "Cannot move bench-press up")
    }

    @Test
    fun `moveExerciseDown throws for the last exercise`() {
        val session = fullBodyA.toOverviewSession()
        val lastId = session.exercises.last().id
        val error = assertFailsWith<IllegalArgumentException> {
            session.moveExerciseDown(lastId)
        }
        assert(error.message == "Cannot move $lastId down")
    }

    @Test
    fun `unknown exercise id throws when moving`() {
        val session = fullBodyA.toOverviewSession()
        val error = assertFailsWith<IllegalArgumentException> {
            session.moveExerciseUp("missing")
        }
        assert(error.message == "Exercise missing is not in the session")
        assertFailsWith<IllegalArgumentException> { session.moveExerciseDown("missing") }
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

    @Test
    fun `moveExerciseDown swaps incomplete exercises during an active session`() {
        val session = fullBodyA.toWorkoutSession()

        val actual = session.moveExerciseDown("bench-press")

        assert(actual.exercises.map { it.id }.take(2) == listOf("squat", "bench-press"))
        assert(actual.exercises.first { it.id == "bench-press" }.state == ExerciseState.InProgress)
        assert(actual.exercises.first().state == ExerciseState.NotStarted)
    }

    @Test
    fun `moveExerciseUp swaps incomplete exercises after a completed one`() {
        val session = fullBodyA.toWorkoutSession().withExerciseCompleted("bench-press")

        val actual = session.moveExerciseUp("assisted-pull-ups")

        assert(
            actual.exercises.map { it.id }.take(3) ==
                listOf("bench-press", "assisted-pull-ups", "squat"),
        )
        assert(actual.exercises.first().state == ExerciseState.Completed)
    }

    @Test
    fun `moveExercise throws for a completed exercise`() {
        val session = fullBodyA.toWorkoutSession().withExerciseCompleted("bench-press")
        val error = assertFailsWith<IllegalArgumentException> {
            session.moveExerciseUp("bench-press")
        }
        assert(error.message == "Cannot move completed exercise bench-press")
        val down = assertFailsWith<IllegalArgumentException> {
            session.moveExerciseDown("bench-press")
        }
        assert(down.message == "Cannot move completed exercise bench-press")
    }

    @Test
    fun `moveExerciseUp throws when the previous exercise is completed`() {
        val session = fullBodyA.toWorkoutSession().withExerciseCompleted("bench-press")
        val error = assertFailsWith<IllegalArgumentException> {
            session.moveExerciseUp("squat")
        }
        assert(error.message == "Cannot move squat up")
    }

    @Test
    fun `moveDirections excludes completed exercises and neighbors`() {
        val session = fullBodyA.toWorkoutSession().withExerciseCompleted("bench-press")
        val completed = session.exercises.first()
        val squat = session.exercises.first { it.id == "squat" }
        val last = session.exercises.last()

        assert(session.moveDirections(completed).isEmpty())
        assert(session.moveDirections(squat) == listOf(ReorderDirection.Down))
        assert(
            session.moveDirections(session.exercises.first { it.id == "assisted-pull-ups" }) ==
                listOf(ReorderDirection.Up, ReorderDirection.Down),
        )
        assert(session.moveDirections(last) == listOf(ReorderDirection.Up))
    }
}

private fun WorkoutSession.withExerciseCompleted(exerciseId: String): WorkoutSession = copy(
    exercises = exercises.map { exercise ->
        if (exercise.id != exerciseId) {
            exercise
        } else {
            exercise.copy(
                warmupSets = exercise.warmupSets?.map { it.copy(setState = SetState.Completed()) },
                sets = exercise.sets.map { it.copy(setState = SetState.Completed()) },
            )
        }
    },
)
