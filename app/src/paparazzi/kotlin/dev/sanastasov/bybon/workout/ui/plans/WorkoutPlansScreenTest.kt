package dev.sanastasov.bybon.workout.ui.plans

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import dev.sanastasov.bybon.paparazzi.bybonSnapshot
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import dev.sanastasov.bybon.workout.domain.upperBodyA
import org.junit.Rule
import org.junit.Test

class WorkoutPlansScreenTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_9,
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    @Test
    fun activeAndDraft() {
        paparazzi.bybonSnapshot {
            WorkoutsTab(
                WorkoutPlansUiState(
                    plans = listOf(
                        WorkoutPlanUi.Active(
                            fullBodyA,
                            lastExecutedLabel = "10 Aug",
                            activeExerciseId = fullBodyA.sets[1].exercise.id,
                        ),
                        WorkoutPlanUi.Draft(
                            fullBodyB,
                            lastExecutedLabel = "1 Oct",
                        ),
                    ),
                ),
                {},
                Modifier.fillMaxSize(),
            )
        }
    }

    @Test
    fun withArchived() {
        paparazzi.bybonSnapshot {
            WorkoutsTab(
                WorkoutPlansUiState(
                    plans = listOf(
                        WorkoutPlanUi.Idle(fullBodyA),
                        WorkoutPlanUi.Idle(fullBodyB),
                        WorkoutPlanUi.Idle(upperBodyA),
                    ),
                    showArchived = true,
                ),
                {},
                Modifier.fillMaxSize(),
            )
        }
    }
}
