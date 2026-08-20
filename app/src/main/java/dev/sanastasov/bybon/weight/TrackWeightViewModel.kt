package dev.sanastasov.bybon.weight

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private val dailyEntries = listOf(
    WeightEntryUi("Yesterday", "65.2kg"),
    WeightEntryUi("Monday", "65.2kg"),
)
val weeklyEntries = listOf(
    WeeklyAverageEntryUi("CW 32", "64.8 kg", "+0.1 vs CW 31"),
    WeeklyAverageEntryUi("CW 31", "64.7 kg", "same as CW 30"),
    WeeklyAverageEntryUi("CW 30", "64.7 kg", "-0.1 vs CW 29")
)

class TrackWeightViewModel {

    val uiState: StateFlow<TrackWeightUiState>
        field = MutableStateFlow(TrackWeightUiState(true, dailyEntries, weeklyEntries))
}