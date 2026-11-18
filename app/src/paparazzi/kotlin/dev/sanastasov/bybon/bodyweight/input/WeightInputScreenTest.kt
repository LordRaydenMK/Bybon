package dev.sanastasov.bybon.bodyweight.input

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.paparazzi.bybonSnapshot
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class WeightInputScreenTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_9,
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    @Test
    fun unsaved() {
        paparazzi.bybonSnapshot {
            WeightInputContent(
                "80.00",
                WeightInputUi(LocalDate.of(2026, 8, 21)),
                {},
            )
        }
    }

    @Test
    fun saved() {
        paparazzi.bybonSnapshot {
            WeightInputContent(
                "65.45",
                WeightInputUi(LocalDate.of(2026, 8, 21), BodyWeight(6545)),
                {},
            )
        }
    }
}
