package dev.sanastasov.bybon

import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import org.junit.Test

class OpenWorkoutSessionNavigationTest {

    private val planId = WorkoutPlanId("full-body-a")
    private val otherPlanId = WorkoutPlanId("full-body-b")

    @Test
    fun `missing session should be opened`() {
        val screens = listOf(Screen.MainScreen)

        assert(!screens.containsWorkoutSession(planId))
    }

    @Test
    fun `session already on the stack should not be opened again`() {
        val screens = listOf(
            Screen.MainScreen,
            Screen.WorkoutSession(planId),
            Screen.ExerciseDetails("bench-press"),
        )

        assert(screens.containsWorkoutSession(planId))
    }

    @Test
    fun `a session for a different plan does not count`() {
        val screens = listOf(
            Screen.MainScreen,
            Screen.WorkoutSession(otherPlanId),
        )

        assert(!screens.containsWorkoutSession(planId))
    }
}
