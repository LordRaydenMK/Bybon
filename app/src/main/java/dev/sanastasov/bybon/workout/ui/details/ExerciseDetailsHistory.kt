package dev.sanastasov.bybon.workout.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.ui.MISSING_LOAD_LABEL
import dev.sanastasov.bybon.workout.ui.formatLoadedSet

internal fun LazyListScope.detailsPersonalRecord(personalRecord: PersonalRecordUi) {
    item(key = "personal-record") {
        DetailsSection("Personal record") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${personalRecord.oneRm.kilograms} kg 1RM",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    personalRecord.dateLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

internal fun LazyListScope.detailsOneRmTrend(
    points: List<ExerciseOneRmPointUi>,
    personalRecordOneRm: Weight?,
) {
    require(points.isNotEmpty()) { "1RM trend requires at least one point" }
    item(key = "one-rm-trend") {
        ExerciseOneRmTrendCard(points, personalRecordOneRm)
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
            RecentSetRow(set)
        }
    }
}

@Composable
private fun RecentSetRow(set: ExerciseDetailsSetUi) {
    val load = historySetLoadLabel(set)
    val oneRm = historySetOneRmLabel(set)
    Row(
        Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "$load  $oneRm" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            load,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.width(24.dp))
        Text(
            oneRm,
            Modifier.widthIn(min = 112.dp),
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
            textAlign = TextAlign.End,
        )
    }
}

internal fun historySetLoadLabel(set: ExerciseDetailsSetUi): String =
    "${set.number}. ${formatLoadedSet(set.weightKg, set.reps)}"

internal fun historySetOneRmLabel(set: ExerciseDetailsSetUi): String =
    set.oneRm?.let { "${it.kilograms} kg 1RM" } ?: MISSING_LOAD_LABEL
