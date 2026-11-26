package dev.sanastasov.bybon.workout.ui.plans

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import dev.sanastasov.bybon.paparazzi.bybonSnapshot
import dev.sanastasov.bybon.workout.domain.fullBodyA
import org.junit.Rule
import org.junit.Test

class EditPlanScreenTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_9,
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    @Test
    fun fullPlan() {
        paparazzi.bybonSnapshot {
            EditPlanContent(fullBodyA, {}, {})
        }
    }

    @Test
    fun singleExercise() {
        paparazzi.bybonSnapshot {
            EditPlanContent(
                fullBodyA.copy(sets = listOf(fullBodyA.sets.first())),
                {},
                {},
            )
        }
    }

    @Test
    fun noWarmups() {
        paparazzi.bybonSnapshot {
            EditPlanContent(
                fullBodyA.copy(
                    sets = listOf(fullBodyA.sets.first { it.exercise.id == "seated-leg-curl" }),
                ),
                {},
                {},
            )
        }
    }
}
