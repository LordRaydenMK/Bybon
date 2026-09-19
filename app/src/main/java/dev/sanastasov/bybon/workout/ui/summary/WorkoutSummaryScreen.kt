package dev.sanastasov.bybon.workout.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutSessionId

@Composable
fun WorkoutModule.WorkoutSummaryScreen(sessionId: WorkoutSessionId, onNavigateBack: () -> Unit) {
    val viewModel = retain {
        WorkoutSummaryViewModel(sessionId, workoutsRepository, it.coroutineScope)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WorkoutSummaryContent(uiState, onNavigateBack)
}

@Composable
private fun WorkoutSummaryContent(
    uiState: WorkoutSummaryUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when (uiState) {
        is WorkoutSummaryUiState.Content -> uiState.title
        else -> "Workout summary"
    }
    Scaffold(
        modifier.fillMaxSize(),
        topBar = { BybonTopAppBar(title, onNavigateBack) },
    ) { contentPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            when (uiState) {
                WorkoutSummaryUiState.Loading -> LoadingIndicator()
                is WorkoutSummaryUiState.Content -> SummaryList(uiState)
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun SummaryList(state: WorkoutSummaryUiState.Content) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        state.note?.let { note ->
            item(key = "session-note") {
                Text(
                    note,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        items(state.exercises, key = { it.id }) { exercise ->
            Card(Modifier.fillMaxWidth()) {
                ExerciseSummaryCard(exercise)
            }
        }
    }
}

@Composable
private fun ExerciseSummaryCard(exercise: WorkoutSummaryExerciseUi) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            exercise.name,
            fontWeight = FontWeight.Bold,
        )
        exercise.note?.let { note ->
            Text(
                note,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(4.dp))
        exercise.sets.forEach { set ->
            CompletedSetRow(set)
        }
    }
}

@Composable
private fun CompletedSetRow(set: WorkoutSummarySetUi) {
    val summary = "${set.number}. ${set.weightKg} kg x ${set.reps} @ ${set.oneRm.kilograms} kg 1RM"
    Row(
        Modifier
            .defaultMinSize(minHeight = 48.dp)
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            summary,
            Modifier.weight(1f),
        )
        Checkbox(
            true,
            null,
            Modifier.padding(horizontal = 10.dp),
        )
    }
}

@Preview
@Composable
private fun WorkoutSummaryContentPreview() {
    Surface {
        WorkoutSummaryContent(
            uiState = WorkoutSummaryUiState.Content(
                title = "Full Body B",
                note = "Friday full body workout",
                exercises = listOf(
                    WorkoutSummaryExerciseUi(
                        id = "rdl-bb",
                        name = "Romanian Deadlift (RDL) (barbell)",
                        sets = listOf(
                            WorkoutSummarySetUi(1, "45", 12, Weight.kilograms(63f)),
                            WorkoutSummarySetUi(2, "45", 12, Weight.kilograms(63f)),
                        ),
                    ),
                    WorkoutSummaryExerciseUi(
                        id = "incline-bench-press-db",
                        name = "Incline Bench Press (dumbbell)",
                        note = "Rep range 11-15",
                        sets = listOf(
                            WorkoutSummarySetUi(1, "20", 13, Weight.kilograms(28.67f)),
                            WorkoutSummarySetUi(2, "20", 11, Weight.kilograms(27.33f)),
                            WorkoutSummarySetUi(3, "20", 8, Weight.kilograms(24.83f)),
                        ),
                    ),
                ),
            ),
            onNavigateBack = {},
        )
    }
}
