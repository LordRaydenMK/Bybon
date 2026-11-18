package dev.sanastasov.bybon.workout.ui.summary

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import dev.sanastasov.bybon.paparazzi.bybonSnapshot
import org.junit.Rule
import org.junit.Test

class WorkoutSummaryScreenTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_9.copy(
            screenHeight = (DeviceConfig.PIXEL_9.screenHeight * 3.35).toInt(),
        ),
        theme = "android:Theme.Material.Light.NoActionBar",
        useDeviceResolution = true,
    )

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
