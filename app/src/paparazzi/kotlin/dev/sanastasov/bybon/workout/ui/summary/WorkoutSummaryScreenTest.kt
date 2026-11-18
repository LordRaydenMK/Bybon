package dev.sanastasov.bybon.workout.ui.summary

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import dev.sanastasov.bybon.paparazzi.bybonSnapshot
import org.junit.Rule
import org.junit.Test

class WorkoutSummaryScreenTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_9,
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    @Test
    fun loading() {
        paparazzi.bybonSnapshot {
            WorkoutSummaryContent(
                WorkoutSummaryUiState.Loading,
                {},
                {},
            )
        }
    }

    @Test
    fun content() {
        paparazzi.bybonSnapshot {
            WorkoutSummaryContent(
                PreviewSummaryUiState,
                {},
                {},
            )
        }
    }
}
