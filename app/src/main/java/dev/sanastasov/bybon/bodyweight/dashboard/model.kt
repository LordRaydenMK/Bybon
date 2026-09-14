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
    val kilograms: Float,
    val isLastSevenDaysFallback: Boolean = false,
)

data class WeightDashboardUiState(
    val logWeightPrompt: LogWeightPrompt = LogWeightPrompt.Hidden,
    val comparison: BodyWeightComparison? = null,
    val dailyEntries: List<BodyWeightEntry>? = null,
    val weeklyAverages: List<WeeklyAverageEntryUi>? = null,
    val weeklyTrend: List<WeeklyTrendPointUi>? = null,
) {

    val dailyHeaderText: String? =
        dailyEntries?.lastOrNull()?.let { entry ->
            "This week (${entry.date.toDisplayDate()}) CW ${entry.date.weekOfYear}"
        }
}
