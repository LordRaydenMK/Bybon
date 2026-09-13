package dev.sanastasov.bybon.workout.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.marcellogalhardo.retained.compose.retain
import dev.sanastasov.bybon.ui.components.BybonTopAppBar
import dev.sanastasov.bybon.workout.WorkoutModule
import dev.sanastasov.bybon.workout.data.sampleFullBodyBCompleted
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun WorkoutModule.WorkoutHistoryScreen(onNavigateBack: () -> Unit) {
    val viewModel = retain {
        WorkoutHistoryViewModel(workoutsRepository, it.coroutineScope)
    }
    val sessions by viewModel.uiState.collectAsStateWithLifecycle()
    WorkoutHistoryContent(sessions, onNavigateBack)
}

@Composable
private fun WorkoutHistoryContent(
    sessions: List<WorkoutSessionHistoryUi>,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier.fillMaxSize(),
        topBar = { BybonTopAppBar("History", onNavigateBack) },
    ) { contentPadding ->
        if (sessions.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("No completed workouts yet")
            }
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(sessions, key = { it.key }) { session ->
                    WorkoutSessionHistoryCard(session)
                }
            }
        }
    }
}

@Composable
private fun WorkoutSessionHistoryCard(session: WorkoutSessionHistoryUi) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                session.planName,
                fontWeight = FontWeight.Bold,
            )
            Text(
                DATE_FORMAT.format(session.date),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            session.exercises.forEach { exercise ->
                Text(exercise.summary())
            }
        }
    }
}

private val DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

private fun ExerciseTopSetUi.summary(): String = buildString {
    append("$name: $weightKg kg x $reps")
    estimatedOneRmKg?.let { oneRm ->
        append(" @ ${formatOneRmKg(oneRm)} kg 1RM")
    }
}

private fun formatOneRmKg(kg: Float): String =
    "%.2f".format(Locale.US, kg).trimEnd('0').trimEnd('.')

@Preview
@Composable
private fun WorkoutHistoryContentPreview() {
    Surface {
        WorkoutHistoryContent(
            sessions = listOf(sampleFullBodyBCompleted).toHistoryUi(),
            onNavigateBack = {},
        )
    }
}

@Preview
@Composable
private fun WorkoutHistoryEmptyPreview() {
    Surface {
        WorkoutHistoryContent(
            sessions = emptyList(),
            onNavigateBack = {},
        )
    }
}
