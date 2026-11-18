package dev.sanastasov.bybon.bodyweight.dashboard

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.paparazzi.bybonSnapshot
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class WeightDashboardTabTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_9,
        theme = "android:Theme.Material.Light.NoActionBar",
        useDeviceResolution = true,
    )

    @Test
    fun prominentPrompt() {
        paparazzi.bybonSnapshot {
            WeightDashboardTab(prominentState(), {})
        }
    }

    @Test
    fun loggedToday() {
        paparazzi.bybonSnapshot {
            WeightDashboardTab(loggedTodayState(), {})
        }
    }
}

private fun prominentState(): WeightDashboardUiState {
    val day = LocalDate.of(2026, 8, 10)
    return WeightDashboardUiState(
        LogWeightPrompt.Prominent,
        BodyWeightComparison(
            "CW 33 average",
            "65 kg",
            PreviousWeekData(32, "+0.5 kg"),
        ),
        listOf(
            BodyWeightEntry(day.minusDays(1), BodyWeight.parseFromString("65.2")),
            BodyWeightEntry(day.minusDays(2), BodyWeight.parseFromString("64.8")),
        ),
        listOf(
            WeeklyAverageEntryUi("CW 32", "64.8 kg", "+0.1 vs CW 31"),
            WeeklyAverageEntryUi("CW 31", "64.7 kg", "same as CW 30"),
            WeeklyAverageEntryUi("CW 30", "64.7 kg", null),
        ),
        previewTrendPoints(),
    )
}

private fun loggedTodayState(): WeightDashboardUiState {
    val today = LocalDate.of(2026, 8, 10)
    return WeightDashboardUiState(
        LogWeightPrompt.Compact,
        BodyWeightComparison(
            "Last 7d average",
            "65.0 kg",
        ),
        listOf(
            BodyWeightEntry(today, BodyWeight.parseFromString("65.2")),
            BodyWeightEntry(today.minusDays(4), BodyWeight.parseFromString("64.8")),
        ),
        listOf(
            WeeklyAverageEntryUi("CW 32", "64.8 kg", "+0.1 vs CW 31"),
        ),
        listOf(
            WeeklyTrendPointUi("32", 0, 64.8f),
            WeeklyTrendPointUi("33", 1, 65.0f, isLastSevenDaysFallback = true),
        ),
    )
}

private fun previewTrendPoints(): List<WeeklyTrendPointUi> {
    val kilograms = listOf(
        67.4f, 67.1f, 66.8f, 66.9f, 66.4f, 66.1f, 65.8f, 65.9f,
        65.5f, 65.3f, 65.0f, 64.8f, 64.9f, 64.7f, 64.8f, 65.0f,
    )
    return kilograms.mapIndexed { index, value ->
        WeeklyTrendPointUi((22 + index).toString(), index, value)
    }
}
