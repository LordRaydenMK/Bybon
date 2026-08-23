package dev.sanastasov.bybon.bodyweight.dashboard

data class WeightEntryUi(val day: String, val value: String)

data class WeeklyAverageEntryUi(val week: String, val value: String, val delta: String)

data class WeightDashboardUiState(
    val showLogWeight: Boolean,
    val dailyEntries: List<WeightEntryUi>,
    val weeklyAverages: List<WeeklyAverageEntryUi>,
)