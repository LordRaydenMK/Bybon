package dev.sanastasov.bybon.workout.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.ui.formatSetPerformance

internal fun LazyListScope.detailsPersonalRecord(personalRecordOneRm: Weight) {
    item(key = "personal-record") {
        DetailsSection("Personal record") {
            Text(
                "${personalRecordOneRm.kilograms} kg 1RM",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

internal fun LazyListScope.detailsOneRmTrend(points: List<ExerciseOneRmPointUi>) {
    require(points.isNotEmpty()) { "1RM trend requires at least one point" }
    item(key = "one-rm-trend") {
        ExerciseOneRmTrendCard(points)
    }
}

internal fun LazyListScope.detailsRecentSessions(sessions: List<ExerciseDetailsSessionUi>) {
    item(key = "recent-history") {
        DetailsSection("Last 3 months") {
            if (sessions.isEmpty()) {
                Text(
                    "No working sets in the last 3 months.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    sessions.forEach { session ->
                        RecentSessionBlock(session)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentSessionBlock(session: ExerciseDetailsSessionUi) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(session.planName, style = MaterialTheme.typography.titleSmall)
        Text(
            session.dateLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        session.sets.forEach { set ->
            Text(
                "${set.number}. ${formatSetPerformance(set.weightKg, set.reps, set.oneRm)}",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
