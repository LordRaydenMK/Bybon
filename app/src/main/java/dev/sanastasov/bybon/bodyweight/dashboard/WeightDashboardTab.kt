package dev.sanastasov.bybon.bodyweight.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.domain.toDisplayDate
import java.time.LocalDate

@Composable
fun WeightDashboardTab(state: WeightDashboardUiState, onLogWeightClicked: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.comparison != null) {
            WeightComparison(state.comparison)
        }

        when (state.logWeightPrompt) {
            LogWeightPrompt.Prominent -> LogWeight(onLogWeightClicked)
            LogWeightPrompt.Compact -> CompactLogWeight(onLogWeightClicked)
            LogWeightPrompt.Hidden -> Unit
        }

        if (state.dailyEntries != null) {
            HeaderEntry(state.dailyHeaderText!!)
            state.dailyEntries.forEach {
                DailyEntry(it)
            }
        }

        if (state.weeklyTrend != null) {
            WeeklyWeightTrendCard(state.weeklyTrend)
        }

        if (state.weeklyAverages != null) {
            HeaderEntry("Previous weeks")
            state.weeklyAverages.forEach {
                WeeklyAverage(it)
            }
        }
    }
}

@Composable
private fun HeaderEntry(text: String) {
    Text(text)
    HorizontalDivider()
}

@Composable
private fun LogWeight(onLogWeightClicked: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Enter today's weight")

            Spacer(Modifier.height(8.dp))

            Button(onLogWeightClicked) {
                Text("Log Weight")
            }
        }
    }
}

@Composable
private fun CompactLogWeight(onLogWeightClicked: () -> Unit) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextButton(onLogWeightClicked) {
            Text("Log Weight")
        }
    }
}

@Composable
private fun WeightComparison(comparison: BodyWeightComparison) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(comparison.title)
            Spacer(Modifier.height(8.dp))

            Text(
                comparison.currentWeightWeight,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))

            comparison.previousWeek?.let {
                Text("${it.weightDelta} since CW ${it.previousWeekNo}")
            }
        }
    }
}

@Composable
private fun WeeklyAverage(entry: WeeklyAverageEntryUi) {
    Row(Modifier.padding(8.dp)) {
        Text(entry.week, Modifier.weight(1f))
        Text(entry.value, Modifier.weight(1f))
        Text(entry.delta ?: "n/a", Modifier.weight(1f))
    }
}

@Composable
private fun DailyEntry(entry: BodyWeightEntry) {
    Row(Modifier.padding(8.dp)) {
        Text(entry.date.toDisplayDate(), Modifier.weight(1f))
        Text("${entry.weight.kilograms} kg", Modifier.weight(1f))
    }
}

@Preview
@Composable
private fun WeightDashboardPreview() {
    val state = WeightDashboardUiState(
        LogWeightPrompt.Prominent,
        BodyWeightComparison(
            "CW 33 average",
            "65 kg",
            PreviousWeekData(32, "+0.5 kg"),
        ),
        listOf(
            BodyWeightEntry(LocalDate.now().minusDays(1), BodyWeight.parseFromString("65.2")),
            BodyWeightEntry(LocalDate.now().minusDays(2), BodyWeight.parseFromString("64.8")),
        ),
        listOf(
            WeeklyAverageEntryUi("CW 32", "64.8 kg", "+0.1 vs CW 31"),
            WeeklyAverageEntryUi("CW 31", "64.7 kg", "same as CW 30"),
            WeeklyAverageEntryUi("CW 30", "64.7 kg", null),
        ),
        previewTrendPoints(),
    )
    Surface {
        WeightDashboardTab(state, {})
    }
}

@Preview
@Composable
private fun WeightDashboardLoggedTodayPreview() {
    val today = LocalDate.of(2026, 8, 10)
    val state = WeightDashboardUiState(
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
    Surface {
        WeightDashboardTab(state, {})
    }
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
