package dev.sanastasov.bybon

import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
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
}
