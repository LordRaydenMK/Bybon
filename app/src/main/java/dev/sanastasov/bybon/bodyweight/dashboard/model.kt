package dev.sanastasov.bybon.bodyweight.dashboard

import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.domain.weekOfYear
import java.time.format.DateTimeFormatter

data class PreviousWeekData(
    val previousWeekNo: Int,
    val weightDelta: String,
)

data class BodyWeightComparison(
    val currentWeekNo: Int,
    val currentWeightWeight: String,
    val previousWeek: PreviousWeekData? = null
)

data class WeeklyAverageEntryUi(val week: String, val value: String, val delta: String?)


private val DAY_MONTH_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM")

data class WeightDashboardUiState(
    val showLogWeight: Boolean,
    val comparison: BodyWeightComparison? = null,
    val dailyEntries: List<BodyWeightEntry>? = null,
    val weeklyAverages: List<WeeklyAverageEntryUi>? = null,
) {

    val dailyHeaderText: String? =
        dailyEntries?.lastOrNull()?.let { entry ->
            "This week (${DAY_MONTH_DATE_FORMAT.format(entry.date)}) CW ${entry.date.weekOfYear}"
        }
}