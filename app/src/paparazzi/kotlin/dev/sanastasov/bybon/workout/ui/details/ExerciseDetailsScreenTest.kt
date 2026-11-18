package dev.sanastasov.bybon.workout.ui.details

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import coil3.ColorImage
import coil3.test.FakeImageLoaderEngine
import dev.sanastasov.bybon.paparazzi.bybonSnapshot
import dev.sanastasov.bybon.paparazzi.installFakeCoil
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.catalogExercise
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ExerciseDetailsScreenTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_9.copy(
            screenHeight = DeviceConfig.PIXEL_9.screenHeight * 3,
        ),
        theme = "android:Theme.Material.Light.NoActionBar",
        useDeviceResolution = true,
    )

    @Before
    fun installFakeCoil() {
        paparazzi.installFakeCoil(
            FakeImageLoaderEngine.Builder()
                .intercept(
                    { it is String && it.endsWith("bench-press-start.webp") },
                    ColorImage(Color.Red.toArgb()),
                )
                .intercept(
                    { it is String && it.endsWith("bench-press-peak.webp") },
                    ColorImage(Color.Green.toArgb()),
                )
                .default(ColorImage(Color.Blue.toArgb()))
                .build(),
        )
    }

    @Test
    fun benchPress() {
        paparazzi.bybonSnapshot {
            ExerciseDetailsContent(benchPressDetails(), {})
        }
    }
}

private fun benchPressDetails(): ExerciseDetailsUiState.Content =
    catalogExercise("bench-press").toDetailsUi().copy(
        personalRecord = PersonalRecordUi(
            Weight.kilograms(78f),
            "20 Sep 2026",
            "60",
            8,
        ),
        oneRmTrend = listOf(
            ExerciseOneRmPointUi(
                "12 Jul",
                70f,
                Weight.kilograms(70f),
                "55",
                8,
                Weight.kilograms(68f),
            ),
            ExerciseOneRmPointUi(
                "2 Aug",
                74f,
                Weight.kilograms(74f),
                "58",
                8,
                Weight.kilograms(72f),
            ),
            ExerciseOneRmPointUi(
                "20 Sep",
                78f,
                Weight.kilograms(78f),
                "60",
                8,
                Weight.kilograms(75f),
            ),
        ),
        recentSessions = listOf(
            ExerciseDetailsSessionUi(
                dateLabel = "20 Sep 2026",
                planName = "Full Body A",
                sets = listOf(
                    ExerciseDetailsSetUi(1, "60", 8, Weight.kilograms(75f)),
                    ExerciseDetailsSetUi(2, "60", 7, Weight.kilograms(72f)),
                ),
            ),
        ),
    )
