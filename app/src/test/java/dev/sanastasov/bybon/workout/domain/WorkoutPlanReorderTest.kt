package dev.sanastasov.bybon.workout.domain

import kotlin.test.assertFailsWith
import org.junit.Test

class WorkoutPlanReorderTest {

    private val plans = listOf(fullBodyA, fullBodyB, upperBodyA)

    @Test
    fun `move down swaps with the next idle plan and leaves archived plans in place`() {
        val actual = plans.movePlan(fullBodyA.id, ReorderDirection.Down) { !it.isArchived }

        assert(actual.map { it.id } == listOf(fullBodyB.id, fullBodyA.id, upperBodyA.id))
    }

    @Test
    fun `move up swaps with the previous idle plan`() {
        val actual = plans.movePlan(fullBodyB.id, ReorderDirection.Up) { !it.isArchived }

        assert(actual.map { it.id } == listOf(fullBodyB.id, fullBodyA.id, upperBodyA.id))
    }

    @Test
    fun `move skips a pinned plan and swaps the idle neighbors`() {
        val extra = fullBodyA.copy(id = WorkoutPlanId("extra"), name = "Extra")
        val list = listOf(fullBodyA, fullBodyB, extra)

        val actual = list.movePlan(extra.id, ReorderDirection.Up) { it.id != fullBodyB.id }

        assert(actual.map { it.id } == listOf(extra.id, fullBodyB.id, fullBodyA.id))
    }

    @Test
    fun `move up throws for the first idle plan`() {
        val error = assertFailsWith<IllegalStateException> {
            plans.movePlan(fullBodyA.id, ReorderDirection.Up) { !it.isArchived }
        }

        assert(error.message == "Cannot move full-body-a up")
    }

    @Test
    fun `move down throws for the last idle plan`() {
        val error = assertFailsWith<IllegalStateException> {
            plans.movePlan(fullBodyB.id, ReorderDirection.Down) { !it.isArchived }
        }

        assert(error.message == "Cannot move full-body-b down")
    }

    @Test
    fun `move throws when the plan is pinned or archived`() {
        val pinned = assertFailsWith<IllegalStateException> {
            plans.movePlan(fullBodyA.id, ReorderDirection.Down) { it.id != fullBodyA.id }
        }
        val archived = assertFailsWith<IllegalStateException> {
            plans.movePlan(upperBodyA.id, ReorderDirection.Up) { !it.isArchived }
        }

        assert(pinned.message == "Cannot move full-body-a")
        assert(archived.message == "Cannot move upper-body-legacy")
    }

    @Test
    fun `move throws when the plan is missing`() {
        val error = assertFailsWith<IllegalStateException> {
            plans.movePlan(WorkoutPlanId("missing"), ReorderDirection.Up) { !it.isArchived }
        }

        assert(error.message == "Plan missing does not exist")
    }
}
