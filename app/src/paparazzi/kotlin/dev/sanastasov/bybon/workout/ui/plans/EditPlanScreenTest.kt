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
            EditPlanContent(EditPlanUiState(fullBodyA, canSave = false), {})
        }
    }

    @Test
    fun singleExercise() {
        paparazzi.bybonSnapshot {
            EditPlanContent(
                EditPlanUiState(
                    fullBodyA.copy(sets = listOf(fullBodyA.sets.first())),
                    canSave = false,
                ),
                {},
            )
        }
    }

    @Test
    fun unsavedChanges() {
        paparazzi.bybonSnapshot {
            EditPlanContent(EditPlanUiState(fullBodyA, canSave = true), {})
        }
    }

    @Test
    fun discardChanges() {
        paparazzi.bybonSnapshot {
            EditPlanContent(
                EditPlanUiState(fullBodyA, canSave = true, confirmDiscard = true),
                {},
            )
        }
    }

    @Test
    fun noWarmups() {
        paparazzi.bybonSnapshot {
            EditPlanContent(
                EditPlanUiState(
                    fullBodyA.copy(
                        sets = listOf(fullBodyA.sets.first { it.exercise.id == "seated-leg-curl" }),
                    ),
                    canSave = false,
                ),
                {},
            )
        }
    }
}
