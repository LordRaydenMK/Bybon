package dev.sanastasov.bybon.bodyweight.dashboard

import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.domain.toDisplayDate
import dev.sanastasov.bybon.domain.weekOfYear

enum class LogWeightPrompt {
    Hidden,
    Prominent,
    Compact,
}

data class PreviousWeekData(
    val previousWeekNo: Int,
    val weightDelta: String,
)

data class BodyWeightComparison(
    val title: String,
    val currentWeightWeight: String,
    val previousWeek: PreviousWeekData? = null,
)

data class WeeklyAverageEntryUi(
    val week: String,
    val value: String,
    val delta: String?,
)

data class WeeklyTrendPointUi(
    val weekLabel: String,
    val weekIndex: Int,
    val kilograms: Float? = null,
    val isLastSevenDaysFallback: Boolean = false,
    val projectedKilograms: Float? = null,
    val maintainLowKilograms: Float? = null,
    val maintainHighKilograms: Float? = null,
    val isFuture: Boolean = false,
)

data class DietPhaseSummaryUi(
    val kindLabel: String,
    val detailLines: List<String>,
    val actionLabel: String,
)

data class WeightDashboardUiState(
    val logWeightPrompt: LogWeightPrompt = LogWeightPrompt.Hidden,
    val comparison: BodyWeightComparison? = null,
    val dailyEntries: List<BodyWeightEntry>? = null,
    val weeklyAverages: List<WeeklyAverageEntryUi>? = null,
    val weeklyTrend: List<WeeklyTrendPointUi>? = null,
    val dietPhaseSummary: DietPhaseSummaryUi? = null,
    val onTrack: Boolean = false,
) {

    val dailyHeaderText: String? =
        dailyEntries?.lastOrNull()?.let { entry ->
            "This week (${entry.date.toDisplayDate()}) CW ${entry.date.weekOfYear}"
        }
}
