package dev.sanastasov.bybon.bodyweight.dashboard

data class PreviousWeekData(
    val previousWeekNo: Int,
    val weightDelta: String,
)

data class BodyWeightComparison(
    val currentWeekNo: Int,
    val currentWeightWeight: String,
    val previousWeek: PreviousWeekData? = null
)

data class WeightEntryUi(val day: String, val value: String)

data class WeeklyAverageEntryUi(val week: String, val value: String, val delta: String?)

data class WeightDashboardUiState(
    val showLogWeight: Boolean,
    val comparison: BodyWeightComparison? = null,
    val dailyEntries: List<WeightEntryUi>? = null,
    val weeklyAverages: List<WeeklyAverageEntryUi>? = null,
)