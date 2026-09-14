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
import java.util.Locale

@Composable
fun WorkoutModule.WorkoutSummaryScreen(sessionKey: String, onNavigateBack: () -> Unit) {
    val viewModel = retain {
        WorkoutSummaryViewModel(sessionKey, workoutsRepository, it.coroutineScope)
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
                WorkoutSummaryUiState.NotFound -> NotFoundMessage()
                is WorkoutSummaryUiState.Content -> SummaryList(uiState.exercises)
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
private fun NotFoundMessage() {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text("Workout not found")
    }
}

@Composable
private fun SummaryList(exercises: List<WorkoutSummaryExerciseUi>) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(exercises, key = { it.id }) { exercise ->
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
        Spacer(Modifier.height(4.dp))
        exercise.sets.forEach { set ->
            CompletedSetRow(set)
        }
    }
}

@Composable
private fun CompletedSetRow(set: WorkoutSummarySetUi) {
    val oneRmLabel = set.estimatedOneRmKg?.let { "@ ${formatOneRmKg(it)} kg 1RM" }
    val summary = buildString {
        append("${set.number}. ${set.weightKg} kg x ${set.reps}")
        if (oneRmLabel != null) {
            append(" $oneRmLabel")
        }
    }
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

private fun formatOneRmKg(kg: Float): String =
    "%.2f".format(Locale.US, kg).trimEnd('0').trimEnd('.')

@Preview
@Composable
private fun WorkoutSummaryContentPreview() {
    Surface {
        WorkoutSummaryContent(
            uiState = WorkoutSummaryUiState.Content(
                title = "Full Body B",
                exercises = listOf(
                    WorkoutSummaryExerciseUi(
                        id = "rdl-bb",
                        name = "Romanian Deadlift (RDL) (barbell)",
                        sets = listOf(
                            WorkoutSummarySetUi(1, "45", 12, 63f),
                            WorkoutSummarySetUi(2, "45", 12, 63f),
                        ),
                    ),
                    WorkoutSummaryExerciseUi(
                        id = "incline-bench-press-db",
                        name = "Incline Bench Press (dumbbell)",
                        sets = listOf(
                            WorkoutSummarySetUi(1, "20", 13, 28.67f),
                            WorkoutSummarySetUi(2, "20", 11, 27.33f),
                            WorkoutSummarySetUi(3, "20", 8, 24.83f),
                        ),
                    ),
                ),
            ),
            onNavigateBack = {},
        )
    }
}
