package dev.sanastasov.bybon.main

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import dev.sanastasov.bybon.bodyweight.dashboard.LogWeightPrompt
import dev.sanastasov.bybon.bodyweight.dashboard.WeightDashboardUiState
import dev.sanastasov.bybon.paparazzi.bybonSnapshot
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import dev.sanastasov.bybon.workout.ui.history.WorkoutHistoryUiState
import dev.sanastasov.bybon.workout.ui.plans.WorkoutPlanUi
import dev.sanastasov.bybon.workout.ui.plans.WorkoutPlansUiState
import org.junit.Rule
import org.junit.Test

class MainScreenTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_9,
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    @Test
    fun workoutPlansTab() {
        paparazzi.bybonSnapshot {
            MainScreenContent(
                selectedIndex = 0,
                onTabSelected = {},
                weightState = WeightDashboardUiState(LogWeightPrompt.Prominent),
                onLogWeightClicked = {},
                plans = WorkoutPlansUiState(
                    listOf(
                        WorkoutPlanUi.Idle(fullBodyA),
                        WorkoutPlanUi.Idle(fullBodyB),
                    ),
                ),
                onWorkoutPlansAction = {},
                historyState = WorkoutHistoryUiState.Empty,
                onImportHistoryClick = {},
                onImportDone = {},
                onSessionClick = {},
                onNavigateToOneRmCalculator = {},
                onNavigateToExerciseLibrary = {},
                onNavigateToAccount = {},
            )
        }
    }
}
