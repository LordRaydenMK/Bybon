package dev.sanastasov.bybon.workout.ui.details

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import coil3.ColorImage
import coil3.test.FakeImageLoaderEngine
import dev.sanastasov.bybon.paparazzi.bybonSnapshot
import dev.sanastasov.bybon.paparazzi.installFakeCoil
import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.ExerciseHistoryWindow
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.catalogExercise
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ExerciseDetailsScreenTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_9.copy(
            screenHeight = DeviceConfig.PIXEL_9.screenHeight * 2,
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

    @Test
    fun assistedPullUps() {
        paparazzi.bybonSnapshot {
            ExerciseDetailsContent(assistedPullUpsDetails(), {})
        }
    }

    @Test
    fun lastTwelveSessions() {
        paparazzi.bybonSnapshot {
            ExerciseDetailsContent(lastTwelveSessionsDetails(), {})
        }
    }
}

private fun benchPressDetails(): ExerciseDetailsUiState.Content =
    catalogExercise("bench-press").toDetailsUi().copy(
        historyWindow = ExerciseHistoryWindow.LastThreeMonths,
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

private fun assistedPullUpsDetails(): ExerciseDetailsUiState.Content =
    catalogExercise("assisted-pull-ups").toDetailsUi().copy(
        historyWindow = ExerciseHistoryWindow.LastThreeMonths,
        personalRecord = PersonalRecordUi(
            Weight.kilograms(92.9f),
            "1 Oct 2026",
            "0",
            6,
        ),
        oneRmTrend = listOf(
            ExerciseOneRmPointUi(
                "10 Aug",
                68.28f,
                Weight.kilograms(68.28f),
                "-25",
                8,
            ),
            ExerciseOneRmPointUi(
                "1 Oct",
                92.9f,
                Weight.kilograms(92.9f),
                "0",
                6,
            ),
        ),
        recentSessions = listOf(
            ExerciseDetailsSessionUi(
                dateLabel = "1 Oct 2026",
                planName = "Full Body A",
                sets = listOf(
                    ExerciseDetailsSetUi(1, "-25", 8, Weight.kilograms(68.28f)),
                    ExerciseDetailsSetUi(2, "0", 6, Weight.kilograms(92.9f)),
                ),
            ),
        ),
    )

private fun lastTwelveSessionsDetails(): ExerciseDetailsUiState.Content =
    ExerciseDefinition(id = "custom-curl", name = "Custom Curl").toDetailsUi().copy(
        historyWindow = ExerciseHistoryWindow.LastTwelveSessions,
        personalRecord = PersonalRecordUi(
            Weight.kilograms(56.25f),
            "18 Oct 2026",
            "45",
            8,
        ),
        oneRmTrend = listOf(
            ExerciseOneRmPointUi(
                "12 Jan",
                50f,
                Weight.kilograms(50f),
                "40",
                8,
                Weight.kilograms(48f),
            ),
            ExerciseOneRmPointUi(
                "3 Apr",
                52f,
                Weight.kilograms(52f),
                "42",
                8,
                Weight.kilograms(50f),
            ),
            ExerciseOneRmPointUi(
                "18 Oct",
                56.25f,
                Weight.kilograms(56.25f),
                "45",
                8,
                Weight.kilograms(54f),
            ),
        ),
        recentSessions = listOf(
            ExerciseDetailsSessionUi(
                dateLabel = "18 Oct 2026",
                planName = "Arms",
                sets = listOf(
                    ExerciseDetailsSetUi(1, "45", 8, Weight.kilograms(56.25f)),
                ),
            ),
            ExerciseDetailsSessionUi(
                dateLabel = "3 Apr 2026",
                planName = "Arms",
                sets = listOf(
                    ExerciseDetailsSetUi(1, "42", 8, Weight.kilograms(52f)),
                ),
            ),
            ExerciseDetailsSessionUi(
                dateLabel = "12 Jan 2026",
                planName = "Arms",
                sets = listOf(
                    ExerciseDetailsSetUi(1, "40", 8, Weight.kilograms(50f)),
                ),
            ),
        ),
    )
