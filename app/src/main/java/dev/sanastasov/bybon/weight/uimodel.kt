package dev.sanastasov.bybon.weight

data class WeightEntryUi(val day: String, val value: String)

data class WeeklyAverageEntryUi(val week: String, val value: String, val delta: String)

data class TrackWeightUiState(
    val showLogWeight: Boolean,
    val dailyEntries: List<WeightEntryUi>,
    val weeklyAverages: List<WeeklyAverageEntryUi>,
)