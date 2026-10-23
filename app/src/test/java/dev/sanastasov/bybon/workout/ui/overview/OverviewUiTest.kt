package dev.sanastasov.bybon.workout.ui.overview

import dev.sanastasov.bybon.workout.data.completedExercise
import dev.sanastasov.bybon.workout.data.completedSession
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import dev.sanastasov.bybon.workout.domain.toOverviewSession
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import java.time.LocalDateTime
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

    @Test
    fun `previousPlanDateLabel formats the last completed date`() {
        assert(previousPlanDateLabel(null) == null)
        assert(
            previousPlanDateLabel(LocalDateTime.of(2026, 1, 1, 12, 0)) == "Previous: 1 Jan 2026",
        )
    }

    @Test
    fun `latestCompletedFor uses the newest completed session of this plan`() {
        val older = completedFullBodyA(LocalDateTime.of(2026, 1, 1, 12, 0))
        val newer = completedFullBodyA(LocalDateTime.of(2026, 8, 13, 18, 0))
        val otherPlan = completedSession(
            planId = fullBodyB.id.id,
            planName = fullBodyB.name,
            startedAt = LocalDateTime.of(2026, 8, 20, 18, 0),
            exercises = listOf(completedExercise("romanian-deadlift", 45f to 12)),
        )
        val inProgress = fullBodyA.toWorkoutSession(
            startedAt = LocalDateTime.of(2026, 8, 21, 18, 0),
        )

        val actual = listOf(older, newer, otherPlan, inProgress).latestCompletedFor(fullBodyA.id)

        assert(actual == newer)
        assert(previousPlanDateLabel(actual?.startedAt) == "Previous: 13 Aug 2026")
    }

    @Test
    fun `latestCompletedFor is null when this plan has never been completed`() {
        val otherPlan = completedSession(
            planId = fullBodyB.id.id,
            planName = fullBodyB.name,
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 0),
            exercises = listOf(completedExercise("romanian-deadlift", 45f to 12)),
        )
        val inProgress = fullBodyA.toWorkoutSession()

        assert(listOf(otherPlan, inProgress).latestCompletedFor(fullBodyA.id) == null)
        assert(emptyList<WorkoutSession>().latestCompletedFor(fullBodyA.id) == null)
    }
}

private fun completedFullBodyA(startedAt: LocalDateTime) = completedSession(
    planId = fullBodyA.id.id,
    planName = fullBodyA.name,
    startedAt = startedAt,
    exercises = listOf(completedExercise("bench-press", 40f to 9)),
)
