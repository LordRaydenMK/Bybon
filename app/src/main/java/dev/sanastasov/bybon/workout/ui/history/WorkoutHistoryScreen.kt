package dev.sanastasov.bybon.workout.ui.history

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.marcellogalhardo.retained.compose.retain
import dev.sanastasov.bybon.ui.components.BybonTopAppBar
import dev.sanastasov.bybon.workout.WorkoutModule
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun WorkoutModule.WorkoutHistoryScreen(onNavigateBack: () -> Unit) {
    val contentResolver = LocalContext.current.contentResolver
    val viewModel = retain {
        WorkoutHistoryViewModel(
            workoutsRepository,
            it.coroutineScope,
            AndroidContentResolverReader(contentResolver),
        )
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val documentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.onAction(WorkoutHistoryAction.OnCsvSelected(uri))
    }

    WorkoutHistoryContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onImportHistoryClick = {
            documentPicker.launch(
                arrayOf(
                    "*/*",
                    "text/*",
                    "text/csv",
                    "text/comma-separated-values",
                    "application/csv",
                )
            )
        },
        onImportDone = { viewModel.onAction(WorkoutHistoryAction.OnImportDone) },
    )
}

@Composable
private fun WorkoutHistoryContent(
    uiState: WorkoutHistoryUiState,
    onNavigateBack: () -> Unit,
    onImportHistoryClick: () -> Unit,
    onImportDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = if (uiState is WorkoutHistoryUiState.Summary) "Import summary" else "History"
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
                WorkoutHistoryUiState.Loading -> LoadingIndicator()
                WorkoutHistoryUiState.Empty -> EmptyHistory(onImportHistoryClick)
                WorkoutHistoryUiState.Importing -> ImportingIndicator()
                is WorkoutHistoryUiState.Summary -> ImportSummary(uiState.summary, onImportDone)
                is WorkoutHistoryUiState.History -> HistoryList(uiState.sessions)
            }
        }
    }
}

@Composable
private fun EmptyHistory(onImportHistoryClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No completed workouts yet")
        Spacer(Modifier.height(16.dp))
        Button(onClick = onImportHistoryClick) {
            Text("Import history")
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
private fun ImportingIndicator() {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            Modifier.semantics { contentDescription = "Importing history" },
        )
    }
}

@Composable
private fun ImportSummary(
    summary: ImportSummaryUi,
    onImportDone: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "${summary.sessionCount} ${sessionsLabel(summary.sessionCount)} imported",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Sessions by plan",
            fontWeight = FontWeight.Bold,
        )
        summary.sessionsByPlan.forEach { plan ->
            Text("${plan.planName}: ${plan.sessionCount}")
        }
        Text("${summary.plansCreatedCount} ${plansLabel(summary.plansCreatedCount)} created")
        Text("${summary.exercisesImportedCount} ${exercisesLabel(summary.exercisesImportedCount)} imported")
        Text("${summary.workingSetCount} working sets imported")
        val firstDate = summary.firstSessionDate
        val lastDate = summary.lastSessionDate
        if (firstDate != null && lastDate != null) {
            Text("From ${DATE_FORMAT.format(firstDate)} to ${DATE_FORMAT.format(lastDate)}")
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onImportDone,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Done")
        }
    }
}

@Composable
private fun HistoryList(sessions: List<WorkoutSessionHistoryUi>) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(sessions, key = { it.key }) { session ->
            WorkoutSessionHistoryCard(session)
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

private fun sessionsLabel(count: Int): String = if (count == 1) "session" else "sessions"

private fun plansLabel(count: Int): String = if (count == 1) "plan" else "plans"

private fun exercisesLabel(count: Int): String = if (count == 1) "exercise" else "exercises"

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
            uiState = WorkoutHistoryUiState.History(
                listOf(
                    WorkoutSessionHistoryUi(
                        key = "full-body-b-preview",
                        planName = "Full Body B",
                        date = LocalDate.of(2026, 8, 13),
                        exercises = listOf(
                            ExerciseTopSetUi(
                                name = "Romanian Deadlift (RDL) (barbell)",
                                weightKg = "45",
                                reps = 12,
                                estimatedOneRmKg = 63f,
                            ),
                        ),
                    ),
                ),
            ),
            onNavigateBack = {},
            onImportHistoryClick = {},
            onImportDone = {},
        )
    }
}

@Preview
@Composable
private fun WorkoutHistoryEmptyPreview() {
    Surface {
        WorkoutHistoryContent(
            uiState = WorkoutHistoryUiState.Empty,
            onNavigateBack = {},
            onImportHistoryClick = {},
            onImportDone = {},
        )
    }
}

@Preview
@Composable
private fun WorkoutHistoryImportSummaryPreview() {
    Surface {
        WorkoutHistoryContent(
            uiState = WorkoutHistoryUiState.Summary(
                ImportSummaryUi(
                    sessionCount = 52,
                    sessionsByPlan = listOf(
                        PlanSessionCountUi("Full Body B", 23),
                        PlanSessionCountUi("Full Body A", 23),
                        PlanSessionCountUi("Upper body A", 3),
                        PlanSessionCountUi("Upper body B", 3),
                    ),
                    plansCreatedCount = 2,
                    exercisesImportedCount = 1,
                    firstSessionDate = LocalDate.of(2026, 2, 17),
                    lastSessionDate = LocalDate.of(2026, 8, 20),
                    workingSetCount = 854,
                ),
            ),
            onNavigateBack = {},
            onImportHistoryClick = {},
            onImportDone = {},
        )
    }
}
