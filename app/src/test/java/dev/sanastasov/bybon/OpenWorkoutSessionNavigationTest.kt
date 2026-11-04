package dev.sanastasov.bybon

import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSessionId
import java.time.LocalDateTime
import org.junit.Test

class OpenWorkoutSessionNavigationTest {

    private val planId = WorkoutPlanId("full-body-a")
    private val otherPlanId = WorkoutPlanId("full-body-b")

    @Test
    fun `missing session is pushed`() {
        val backStack = mutableListOf<Screen>(Screen.MainScreen)

        openWorkoutSessionIfMissing(backStack, planId)

        assert(backStack == listOf(Screen.MainScreen, Screen.WorkoutSession(planId)))
    }

    @Test
    fun `existing session is not pushed again`() {
        val session = Screen.WorkoutSession(planId)
        val backStack = mutableListOf(
            Screen.MainScreen,
            session,
            Screen.ExerciseDetails("bench-press"),
        )

        openWorkoutSessionIfMissing(backStack, planId)

        assert(
            backStack == listOf(
                Screen.MainScreen,
                session,
                Screen.ExerciseDetails("bench-press"),
            ),
        )
    }

    @Test
    fun `session for another plan is still pushed`() {
        val backStack = mutableListOf<Screen>(
            Screen.MainScreen,
            Screen.WorkoutSession(otherPlanId),
        )

        openWorkoutSessionIfMissing(backStack, planId)

        assert(backStack.last() == Screen.WorkoutSession(planId))
    }

    @Test
    fun `completed notification replaces the session with summary`() {
        val sessionId = WorkoutSessionId(planId, LocalDateTime.of(2026, 1, 1, 12, 0))
        val backStack = mutableListOf<Screen>(
            Screen.MainScreen,
            Screen.WorkoutSession(planId),
        )

        openWorkoutFromNotification(backStack, planId, sessionId)

        assert(
            backStack == listOf(
                Screen.MainScreen,
                Screen.WorkoutSummary(sessionId),
            ),
        )
    }

    @Test
    fun `completed notification does not push summary twice`() {
        val sessionId = WorkoutSessionId(planId, LocalDateTime.of(2026, 1, 1, 12, 0))
        val summary = Screen.WorkoutSummary(sessionId)
        val backStack = mutableListOf<Screen>(Screen.MainScreen, summary)

        openWorkoutFromNotification(backStack, planId, sessionId)

        assert(backStack == listOf(Screen.MainScreen, summary))
    }

    @Test
    fun `in-progress notification still opens the session`() {
        val backStack = mutableListOf<Screen>(Screen.MainScreen)

        openWorkoutFromNotification(backStack, planId, completedSessionId = null)

        assert(backStack == listOf(Screen.MainScreen, Screen.WorkoutSession(planId)))
    }
}
