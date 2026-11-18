package dev.sanastasov.bybon.workout.ui.library

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import coil3.ColorImage
import coil3.test.FakeImageLoaderEngine
import dev.sanastasov.bybon.paparazzi.bybonSnapshot
import dev.sanastasov.bybon.paparazzi.installFakeCoil
import dev.sanastasov.bybon.workout.domain.BodyPart
import dev.sanastasov.bybon.workout.domain.Equipment
import dev.sanastasov.bybon.workout.domain.catalogExercise
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ExerciseLibraryScreenTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_9,
        theme = "android:Theme.Material.Light.NoActionBar",
        useDeviceResolution = true,
    )

    @Before
    fun installFakeCoil() {
        paparazzi.installFakeCoil(
            FakeImageLoaderEngine.Builder()
                .default(ColorImage(Color.Blue.toArgb()))
                .build(),
        )
    }

    @Test
    fun picker() {
        paparazzi.bybonSnapshot {
            ExerciseLibraryContent(pickerState(), {}, {})
        }
    }

    @Test
    fun emptyFilter() {
        paparazzi.bybonSnapshot {
            ExerciseLibraryContent(emptyFilterState(), {}, {})
        }
    }
}

private fun libraryCatalog() = listOf(
    catalogExercise("bench-press"),
    catalogExercise("incline-db-curl"),
    catalogExercise("iso-lat-row"),
)

private fun pickerState(): ExerciseLibraryUiState =
    libraryCatalog().toLibraryUiState(
        selectedExerciseId = "bench-press",
        existingExerciseIds = listOf("iso-lat-row"),
    )

private fun emptyFilterState(): ExerciseLibraryUiState =
    libraryCatalog().toLibraryUiState(
        filters = ExerciseLibraryFilters(
            bodyParts = setOf(BodyPart.LowerArms),
            equipment = setOf(Equipment.AssistedBodyWeight),
        ),
    )
