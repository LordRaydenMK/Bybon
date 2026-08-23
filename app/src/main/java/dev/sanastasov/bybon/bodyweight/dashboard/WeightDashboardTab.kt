package dev.sanastasov.bybon.bodyweight.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WeightDashboardTab(state: WeightDashboardUiState, onLogWeightClicked: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (state.showLogWeight) {
            LogWeight(onLogWeightClicked)
        }

        if (state.comparison != null) {
            WeightComparison(state.comparison)
        }

        if (state.dailyEntries != null) {
            HeaderEntry("This Week (17 Aug) CW 34")
            state.dailyEntries.forEach {
                DailyEntry(it)
            }
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
            horizontalAlignment = Alignment.CenterHorizontally
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
private fun WeightComparison(comparison: BodyWeightComparison) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("CW ${comparison.currentWeekNo} average")
            Spacer(Modifier.height(8.dp))

            Text(
                comparison.currentWeightWeight,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
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
        Text(entry.delta.orEmpty(), Modifier.weight(1f))
    }
}

@Composable
private fun DailyEntry(entry: WeightEntryUi) {
    Row(Modifier.padding(8.dp)) {
        Text(entry.day, Modifier.weight(1f))
        Text(entry.value, Modifier.weight(1f))
    }
}

@Preview
@Composable
private fun WeightDashboardPreview() {
    val state = WeightDashboardUiState(
        true,
        BodyWeightComparison(
            33,
            "65 kg",
            PreviousWeekData(32, "+0.5 kg")
        ),
        listOf(
            WeightEntryUi("Yesterday", "65.2kg"),
            WeightEntryUi("Monday", "65.2kg"),
        ),
        listOf(
            WeeklyAverageEntryUi("CW 32", "64.8 kg", "+0.1 vs CW 31"),
            WeeklyAverageEntryUi("CW 31", "64.7 kg", "same as CW 30"),
            WeeklyAverageEntryUi("CW 30", "64.7 kg", "-0.1 vs CW 29")
        )
    )
    Surface {
        WeightDashboardTab(state, {})
    }
}