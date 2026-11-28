package dev.sanastasov.bybon.workout.domain

import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.minutes
import org.junit.Test

class WorkoutPlanEditingTest {

    @Test
    fun `rename trims the name and keeps the plan id`() {
        val actual = fullBodyA.rename("  Push Day  ")

        assert(actual.name == "Push Day")
        assert(actual.id == fullBodyA.id)
        assert(actual.description == fullBodyA.description)
        assert(actual.sets == fullBodyA.sets)
    }

    @Test
    fun `rename rejects a blank name`() {
        val error = assertFailsWith<IllegalArgumentException> {
            fullBodyA.rename("   ")
        }
        assert(error.message == "Plan name must not be blank")
    }

    @Test
    fun `duplicate assigns a new id and prefixes the name`() {
        val source = fullBodyA.copy(
            description = null,
            isArchived = true,
            sets = fullBodyA.sets.mapIndexed { index, planned ->
                if (index == 0) planned.copy(notes = listOf("Pause")) else planned
            },
        )

        val actual = source.duplicate(WorkoutPlanId("full-body-a-copy"))

        assert(actual.id == WorkoutPlanId("full-body-a-copy"))
        assert(actual.name == "Copy of Full Body A")
        assert(actual.description == null)
        assert(actual.isArchived)
        assert(actual.sets == source.sets)
        assert(source.id == fullBodyA.id)
        assert(source.name == fullBodyA.name)
    }

    @Test
    fun `duplicating a copy prefixes that copy name`() {
        val copy = fullBodyA.duplicate(WorkoutPlanId("copy"))

        val again = copy.duplicate(WorkoutPlanId("copy-2"))

        assert(again.name == "Copy of Copy of Full Body A")
        assert(again.id == WorkoutPlanId("copy-2"))
        assert(again.sets == fullBodyA.sets)
        assert(again.description == fullBodyA.description)
    }

    @Test
    fun `duplicate rejects reusing the plan id`() {
        val error = assertFailsWith<IllegalArgumentException> {
            fullBodyA.duplicate(fullBodyA.id)
        }
        assert(error.message == "Duplicated plan needs a new id")
    }

    @Test
    fun `withNote stores a trimmed note`() {
        val actual = fullBodyA.withNote("  Morning session  ")

        assert(actual.description == "Morning session")
        assert(actual.name == fullBodyA.name)
        assert(actual.sets == fullBodyA.sets)
    }

    @Test
    fun `withNote clears the note when the text is blank`() {
        val actual = fullBodyA.withNote("   ")

        assert(actual.description == null)
        assert(actual.name == fullBodyA.name)
    }

    @Test
    fun `withNote adds a note when the plan has none`() {
        val actual = fullBodyA.copy(description = null).withNote("Easy day")

        assert(actual.description == "Easy day")
    }

    @Test
    fun `addWorkSet appends a work set and leaves warmups unchanged`() {
        val actual = fullBodyA.addWorkSet("bench-press")
        val bench = actual.sets.first()

        assert(bench.sets == 4)
        assert(bench.warmupSets == 3)
        assert(actual.sets.drop(1) == fullBodyA.sets.drop(1))
    }

    @Test
    fun `removeLastWorkSet drops the last work set`() {
        val actual = fullBodyA.removeLastWorkSet("seated-leg-curl")
        val curl = actual.sets.first { it.exercise.id == "seated-leg-curl" }

        assert(curl.sets == 2)
        assert(curl.warmupSets == 0)
        assert(actual.sets.first() == fullBodyA.sets.first())
    }

    @Test
    fun `removeLastWorkSet throws when only one work set remains`() {
        val singleSet = fullBodyA.copy(
            sets = listOf(
                fullBodyA.sets.first { it.exercise.id == "seated-leg-curl" }.copy(sets = 1),
            ),
        )

        val error = assertFailsWith<IllegalStateException> {
            singleSet.removeLastWorkSet("seated-leg-curl")
        }
        assert(
            error.message ==
                "Cannot remove last work set from seated-leg-curl; only one remains",
        )
    }

    @Test
    fun `addWarmupSet appends a warmup and leaves work sets unchanged`() {
        val actual = fullBodyA.addWarmupSet("seated-leg-curl")
        val curl = actual.sets.first { it.exercise.id == "seated-leg-curl" }

        assert(curl.warmupSets == 1)
        assert(curl.sets == 3)
        assert(actual.sets.first() == fullBodyA.sets.first())
    }

    @Test
    fun `removeLastWarmupSet drops the last warmup`() {
        val actual = fullBodyA.removeLastWarmupSet("bench-press")
        val bench = actual.sets.first()

        assert(bench.warmupSets == 2)
        assert(bench.sets == 3)
        assert(actual.sets.drop(1) == fullBodyA.sets.drop(1))
    }

    @Test
    fun `removeLastWarmupSet throws when no warmup remains`() {
        val error = assertFailsWith<IllegalStateException> {
            fullBodyA.removeLastWarmupSet("seated-leg-curl")
        }
        assert(error.message == "Cannot remove last warmup set from seated-leg-curl; none remain")
    }

    @Test
    fun `removeExercise drops the matching exercise`() {
        val actual = fullBodyA.removeExercise("seated-leg-curl")

        assert(actual.sets.none { it.exercise.id == "seated-leg-curl" })
        assert(actual.sets.size == fullBodyA.sets.size - 1)
        assert(actual.sets.first() == fullBodyA.sets.first())
    }

    @Test
    fun `addExercise appends a planned exercise with defaults`() {
        val curl = catalogExercise("incline-db-curl")
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
        val bench = catalogExercise("bench-press")
        val error = assertFailsWith<IllegalStateException> {
            fullBodyA.addExercise(bench)
        }
        assert(error.message == "Exercise bench-press is already on the plan")
    }

    @Test
    fun `unknown exercise id throws`() {
        val error = assertFailsWith<IllegalStateException> {
            fullBodyA.addWorkSet("missing")
        }
        assert(error.message == "Exercise missing is not in the plan")
        assertFailsWith<IllegalStateException> { fullBodyA.removeLastWorkSet("missing") }
        assertFailsWith<IllegalStateException> { fullBodyA.addWarmupSet("missing") }
        assertFailsWith<IllegalStateException> { fullBodyA.removeLastWarmupSet("missing") }
        assertFailsWith<IllegalStateException> { fullBodyA.removeExercise("missing") }
        assertFailsWith<IllegalStateException> { fullBodyA.moveExerciseUp("missing") }
        assertFailsWith<IllegalStateException> { fullBodyA.moveExerciseDown("missing") }
    }

    @Test
    fun `moveExerciseDown swaps with the next exercise`() {
        val actual = fullBodyA.moveExerciseDown("bench-press")

        assert(actual.sets.map { it.exercise.id }.take(2) == listOf("squat", "bench-press"))
        assert(actual.sets.drop(2) == fullBodyA.sets.drop(2))
    }

    @Test
    fun `moveExerciseUp swaps with the previous exercise`() {
        val actual = fullBodyA.moveExerciseUp("squat")

        assert(actual.sets.map { it.exercise.id }.take(2) == listOf("squat", "bench-press"))
        assert(actual.sets.drop(2) == fullBodyA.sets.drop(2))
    }

    @Test
    fun `moveExerciseUp throws for the first exercise`() {
        val error = assertFailsWith<IllegalStateException> {
            fullBodyA.moveExerciseUp("bench-press")
        }
        assert(error.message == "Cannot move bench-press up")
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
        val plan = fullBodyA.moveExerciseDown("bench-press")
        val session = plan.toWorkoutSession()

        assert(session.exercises.map { it.id }.take(2) == listOf("squat", "bench-press"))
    }

    @Test
    fun `added work set is used when creating a session`() {
        val plan = fullBodyA.addWorkSet("bench-press")
        val session = plan.toWorkoutSession()

        assert(session.exercises.first().sets.size == 4)
        assert(session.exercises.first().warmupSets?.size == 3)
    }

    @Test
    fun `added warmup set is used when creating a session`() {
        val plan = fullBodyA.addWarmupSet("seated-leg-curl")
        val session = plan.toWorkoutSession()
        val curl = session.exercises.first { it.id == "seated-leg-curl" }

        assert(curl.warmupSets?.size == 1)
        assert(curl.sets.size == 3)
    }

    @Test
    fun `removed warmup set is omitted when creating a session`() {
        val plan = fullBodyA.removeLastWarmupSet("bench-press")
        val session = plan.toWorkoutSession()

        assert(session.exercises.first().warmupSets?.size == 2)
        assert(session.exercises.first().sets.size == 3)
    }

    @Test
    fun `planned exercise requires at least one work set`() {
        val bench = catalogExercise("bench-press")
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

    @Test
    fun `plan requires at least one exercise`() {
        val error = assertFailsWith<IllegalArgumentException> {
            WorkoutPlan(
                id = WorkoutPlanId("empty"),
                name = "Empty",
                description = null,
                sets = emptyList(),
            )
        }
        assert(error.message == "Plan must contain at least one exercise")
    }

    @Test
    fun `removeExercise throws when only one exercise remains`() {
        val single = fullBodyA.copy(sets = listOf(fullBodyA.sets.first()))
        val error = assertFailsWith<IllegalStateException> {
            single.removeExercise("bench-press")
        }
        assert(error.message == "Cannot remove last exercise from the plan")
    }
}
