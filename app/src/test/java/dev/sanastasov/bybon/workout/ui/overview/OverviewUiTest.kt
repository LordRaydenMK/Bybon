package dev.sanastasov.bybon.workout.ui.overview

import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.toOverviewSession
import org.junit.Test

class OverviewUiTest {

    @Test
    fun `exerciseOverflow disables move up on the first card`() {
        val overflow = fullBodyA.toOverviewSession().exerciseOverflow(0) {}!!

        assert(!overflow.canMoveUp)
        assert(overflow.canMoveDown)
    }

    @Test
    fun `exerciseOverflow disables move down on the last card`() {
        val session = fullBodyA.toOverviewSession()
        val overflow = session.exerciseOverflow(session.exercises.lastIndex) {}!!

        assert(overflow.canMoveUp)
        assert(!overflow.canMoveDown)
    }

    @Test
    fun `exerciseOverflow is hidden for a single exercise session`() {
        val session = fullBodyA.toOverviewSession().let { draft ->
            draft.copy(exercises = listOf(draft.exercises.first()))
        }

        assert(session.exerciseOverflow(0) {} == null)
    }
}
