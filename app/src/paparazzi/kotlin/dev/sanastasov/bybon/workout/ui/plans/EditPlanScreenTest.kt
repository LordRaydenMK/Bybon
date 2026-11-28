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
            EditPlanContent(EditPlanUiState(plan = fullBodyA, loadedPlan = fullBodyA), {})
        }
    }

    @Test
    fun singleExercise() {
        paparazzi.bybonSnapshot {
            val plan = fullBodyA.copy(sets = listOf(fullBodyA.sets.first()))
            EditPlanContent(EditPlanUiState(plan = plan, loadedPlan = plan), {})
        }
    }

    @Test
    fun unsavedChanges() {
        paparazzi.bybonSnapshot {
            EditPlanContent(
                EditPlanUiState(
                    plan = fullBodyA,
                    loadedPlan = fullBodyA.copy(name = "Saved"),
                ),
                {},
            )
        }
    }

    @Test
    fun discardChanges() {
        paparazzi.bybonSnapshot {
            EditPlanContent(
                EditPlanUiState(
                    plan = fullBodyA,
                    loadedPlan = fullBodyA.copy(name = "Saved"),
                    confirmDiscard = true,
                ),
                {},
            )
        }
    }

    @Test
    fun noNote() {
        paparazzi.bybonSnapshot {
            val plan = fullBodyA.copy(
                description = null,
                sets = listOf(fullBodyA.sets.first()),
            )
            EditPlanContent(EditPlanUiState(plan = plan, loadedPlan = plan), {})
        }
    }

    @Test
    fun noWarmups() {
        paparazzi.bybonSnapshot {
            val plan = fullBodyA.copy(
                sets = listOf(fullBodyA.sets.first { it.exercise.id == "seated-leg-curl" }),
            )
            EditPlanContent(EditPlanUiState(plan = plan, loadedPlan = plan), {})
        }
    }
}
