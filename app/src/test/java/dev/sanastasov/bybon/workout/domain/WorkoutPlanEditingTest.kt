package dev.sanastasov.bybon.workout.domain

import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.minutes
import org.junit.Test

class WorkoutPlanEditingTest {

    @Test
    fun `addWorkSet appends a work set and leaves warmups unchanged`() {
        val actual = fullBodyA.addWorkSet("bench-press-bb")
        val bench = actual.sets.first()

        assert(bench.sets == 4)
        assert(bench.warmupSets == 3)
        assert(actual.sets.drop(1) == fullBodyA.sets.drop(1))
    }

    @Test
    fun `removeLastWorkSet drops the last work set`() {
        val actual = fullBodyA.removeLastWorkSet("leg-curl")
        val curl = actual.sets.first { it.exercise.id == "leg-curl" }

        assert(curl.sets == 2)
        assert(curl.warmupSets == 0)
        assert(actual.sets.first() == fullBodyA.sets.first())
    }

    @Test
    fun `removeLastWorkSet throws when only one work set remains`() {
        val singleSet = fullBodyA.copy(
            sets = listOf(fullBodyA.sets.first { it.exercise.id == "leg-curl" }.copy(sets = 1)),
        )

        val error = assertFailsWith<IllegalStateException> {
            singleSet.removeLastWorkSet("leg-curl")
        }
        assert(error.message == "Cannot remove last work set from leg-curl; only one remains")
    }

    @Test
    fun `removeExercise drops the matching exercise`() {
        val actual = fullBodyA.removeExercise("leg-curl")

        assert(actual.sets.none { it.exercise.id == "leg-curl" })
        assert(actual.sets.size == fullBodyA.sets.size - 1)
        assert(actual.sets.first() == fullBodyA.sets.first())
    }

    @Test
    fun `addExercise appends a planned exercise with defaults`() {
        val curl = catalogExercise("incline-curl-db")
        val actual = fullBodyA.addExercise(curl)
        val added = actual.sets.last()

        assert(added.exercise == curl)
        assert(added.sets == 3)
        assert(added.warmupSets == 0)
        assert(added.repRange == 8..12)
        assert(added.restAfterWorkSet == 1.minutes)
        assert(actual.sets.dropLast(1) == fullBodyA.sets)
    }

    @Test
    fun `addExercise throws when the exercise is already on the plan`() {
        val bench = catalogExercise("bench-press-bb")
        val error = assertFailsWith<IllegalStateException> {
            fullBodyA.addExercise(bench)
        }
        assert(error.message == "Exercise bench-press-bb is already on the plan")
    }

    @Test
    fun `unknown exercise id throws`() {
        val error = assertFailsWith<IllegalStateException> {
            fullBodyA.addWorkSet("missing")
        }
        assert(error.message == "Exercise missing is not in the plan")
        assertFailsWith<IllegalStateException> { fullBodyA.removeLastWorkSet("missing") }
        assertFailsWith<IllegalStateException> { fullBodyA.removeExercise("missing") }
        assertFailsWith<IllegalStateException> { fullBodyA.moveExerciseUp("missing") }
        assertFailsWith<IllegalStateException> { fullBodyA.moveExerciseDown("missing") }
    }

    @Test
    fun `moveExerciseDown swaps with the next exercise`() {
        val actual = fullBodyA.moveExerciseDown("bench-press-bb")

        assert(actual.sets.map { it.exercise.id }.take(2) == listOf("squat-bb", "bench-press-bb"))
        assert(actual.sets.drop(2) == fullBodyA.sets.drop(2))
    }

    @Test
    fun `moveExerciseUp swaps with the previous exercise`() {
        val actual = fullBodyA.moveExerciseUp("squat-bb")

        assert(actual.sets.map { it.exercise.id }.take(2) == listOf("squat-bb", "bench-press-bb"))
        assert(actual.sets.drop(2) == fullBodyA.sets.drop(2))
    }

    @Test
    fun `moveExerciseUp throws for the first exercise`() {
        val error = assertFailsWith<IllegalStateException> {
            fullBodyA.moveExerciseUp("bench-press-bb")
        }
        assert(error.message == "Cannot move bench-press-bb up")
    }

    @Test
    fun `moveExerciseDown throws for the last exercise`() {
        val lastId = fullBodyA.sets.last().exercise.id
        val error = assertFailsWith<IllegalStateException> {
            fullBodyA.moveExerciseDown(lastId)
        }
        assert(error.message == "Cannot move $lastId down")
    }

    @Test
    fun `moved exercise is used when creating a session`() {
        val plan = fullBodyA.moveExerciseDown("bench-press-bb")
        val session = plan.toWorkoutSession()

        assert(session.exercises.map { it.id }.take(2) == listOf("squat-bb", "bench-press-bb"))
    }

    @Test
    fun `added work set is used when creating a session`() {
        val plan = fullBodyA.addWorkSet("bench-press-bb")
        val session = plan.toWorkoutSession()

        assert(session.exercises.first().sets.size == 4)
        assert(session.exercises.first().warmupSets?.size == 3)
    }

    @Test
    fun `planned exercise requires at least one work set`() {
        val bench = catalogExercise("bench-press-bb")
        val error = assertFailsWith<IllegalArgumentException> {
            PlanedExercise(
                exercise = bench,
                warmupSets = 0,
                sets = 0,
                repRange = 8..10,
            )
        }
        assert(error.message == "sets must be >= 1")
    }
}
