package dev.sanastasov.bybon

import androidx.navigation3.runtime.NavKey
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen : NavKey {

    @Serializable
    data object MainScreen : Screen

    @Serializable
    data class WorkoutOverview(
        val planId: WorkoutPlanId,
    ) : Screen

    @Serializable
    data class WorkoutSession(
        val planId: WorkoutPlanId,
    ) : Screen

    @Serializable
    data object WeightEntryScreen : Screen

    @Serializable
    data object WorkoutHistory : Screen

    @Serializable
    data class WorkoutSummary(
        val sessionKey: String,
    ) : Screen
}
