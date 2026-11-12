package dev.sanastasov.bybon.workout.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.marcellogalhardo.retained.compose.retain
import dev.sanastasov.bybon.ui.collectEffectWithLifecycle
import dev.sanastasov.bybon.ui.components.BybonTopAppBar
import dev.sanastasov.bybon.workout.WorkoutModule
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutSessionId
import dev.sanastasov.bybon.workout.ui.details.ExerciseOneRmPointUi
import dev.sanastasov.bybon.workout.ui.details.ExerciseOneRmTrendCard
import dev.sanastasov.bybon.workout.ui.details.PersonalRecordUi

@Composable
fun WorkoutModule.WorkoutSummaryScreen(
    sessionId: WorkoutSessionId,
    onNavigateBack: () -> Unit,
    onNavigateToExerciseDetails: (String) -> Unit,
) {
    val viewModel = retain {
        WorkoutSummaryViewModel(sessionId, workoutsRepository, it.coroutineScope)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    viewModel.effects.collectEffectWithLifecycle { effect ->
        when (effect) {
            is WorkoutSummaryEffect.OpenExerciseDetails ->
                onNavigateToExerciseDetails(effect.exerciseId)

            WorkoutSummaryEffect.NavigateBack -> onNavigateBack()
        }
    }
    WorkoutSummaryContent(uiState, viewModel::onAction, onNavigateBack)
}

@Composable
private fun WorkoutSummaryContent(
    uiState: WorkoutSummaryUiState,
    onAction: (WorkoutSummaryAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val title = when (uiState) {
        is WorkoutSummaryUiState.Content -> uiState.title
        else -> "Workout summary"
    }
    Scaffold(
        modifier.fillMaxSize(),
        topBar = {
            BybonTopAppBar(
                title,
                onNavigateBack,
                actions = {
                    SummaryOverflowMenu(onDeleteSession = { showDeleteDialog = true })
                },
            )
        },
    ) { contentPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            when (uiState) {
                WorkoutSummaryUiState.Loading -> LoadingIndicator()
                is WorkoutSummaryUiState.Content -> SummaryList(uiState, onAction)
            }
        }
    }
    if (showDeleteDialog) {
        DeleteSessionDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                onAction(WorkoutSummaryAction.OnDeleteSession)
            },
        )
    }
}

@Composable
private fun SummaryOverflowMenu(onDeleteSession: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton({ expanded = true }) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = "More options for workout summary",
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Delete session") },
                onClick = {
                    expanded = false
                    onDeleteSession()
                },
            )
        }
    }
}

@Composable
private fun DeleteSessionDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete session?") },
        text = { Text("This permanently removes this workout and all logged sets.") },
        confirmButton = {
            TextButton(onConfirm) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onDismiss) { Text("Keep session") }
        },
    )
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
private fun SummaryList(
    state: WorkoutSummaryUiState.Content,
    onAction: (WorkoutSummaryAction) -> Unit,
) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "session-summary") {
            WorkoutSessionSummaryCard(state.sessionSummary)
        }
        state.note?.let { note ->
            item(key = "session-note") {
                Text(
                    note,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        state.exercises.forEach { exercise ->
            item(key = exercise.id) {
                Card(Modifier.fillMaxWidth()) {
                    WorkoutSummaryExerciseCard(exercise, onAction)
                }
            }
            if (exercise.oneRmTrend.isNotEmpty()) {
                item(key = "${exercise.id}-trend") {
                    ExerciseOneRmTrendCard(
                        exercise.oneRmTrend,
                        exercise.personalRecord?.oneRm,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun WorkoutSummaryContentPreview() {
    Surface {
        WorkoutSummaryContent(PreviewSummaryUiState, onAction = {}, onNavigateBack = {})
    }
}

private val PreviewSummaryUiState = WorkoutSummaryUiState.Content(
    title = "Full Body B",
    date = "13 Aug 2026",
    startTime = "18:25",
    duration = "47m",
    sessionSummary = WorkoutSessionSummaryUi(
        subtitle = "Full Body B · 13 Aug 2026 · Start 18:25",
        duration = "47m",
        exerciseCount = 2,
        workingSetCount = 5,
        totalReps = 63,
        volumeLoadLabel = "1762.5 kg",
        setPerformance = SetPerformanceUi(improved = 5, maintained = 0, declined = 0),
        exercisePerformance = listOf(
            ExerciseAverageOneRmUi("Romanian Deadlift", "↑ 5.14 kg"),
            ExerciseAverageOneRmUi("Incline Dumbbell Press", "↑ 0.58 kg"),
        ),
        musclesTrained = listOf("Chest", "Back", "Hamstrings", "Glutes"),
    ),
    note = "Friday full body workout",
    exercises = listOf(
        WorkoutSummaryExerciseUi(
            id = "romanian-deadlift",
            name = "Romanian Deadlift",
            sets = listOf(
                WorkoutSummarySetUi(null, "20", 8, null, isWarmup = true),
                WorkoutSummarySetUi(null, "35", 4, null, isWarmup = true),
                WorkoutSummarySetUi(
                    1,
                    "45",
                    12,
                    Weight.kilograms(63f),
                    previousOneRm = Weight.kilograms(57.86f),
                ),
                WorkoutSummarySetUi(
                    2,
                    "45",
                    12,
                    Weight.kilograms(63f),
                    previousOneRm = Weight.kilograms(57.86f),
                ),
            ),
            volumeLoadLabel = "1080 kg",
            personalRecord = PersonalRecordUi(Weight.kilograms(63f), "13 Aug 2026"),
            previous = WorkoutSummaryPreviousUi(
                workoutLabel = "Previous workout: 6 Aug 2026 · 7 days ago",
                topSetOneRm = Weight.kilograms(57.86f),
                averageOneRm = Weight.kilograms(57.86f),
            ),
            oneRmTrend = listOf(
                ExerciseOneRmPointUi(
                    "12 Jul",
                    55f,
                    Weight.kilograms(55f),
                    "40",
                    10,
                    Weight.kilograms(53f),
                ),
                ExerciseOneRmPointUi(
                    "2 Aug",
                    58f,
                    Weight.kilograms(58f),
                    "42",
                    10,
                    Weight.kilograms(56f),
                ),
                ExerciseOneRmPointUi(
                    "13 Aug",
                    63f,
                    Weight.kilograms(63f),
                    "45",
                    12,
                    Weight.kilograms(63f),
                ),
            ),
        ),
        WorkoutSummaryExerciseUi(
            id = "incline-db-press",
            name = "Incline Dumbbell Press",
            notes = listOf("Rep range 11-15"),
            sets = listOf(
                WorkoutSummarySetUi(null, "10", 8, null, isWarmup = true),
                WorkoutSummarySetUi(null, "17.5", 3, null, isWarmup = true),
                WorkoutSummarySetUi(
                    1,
                    "17.5",
                    15,
                    Weight.kilograms(26.25f),
                    previousOneRm = Weight.kilograms(25.67f),
                ),
                WorkoutSummarySetUi(
                    2,
                    "17.5",
                    13,
                    Weight.kilograms(25.08f),
                    previousOneRm = Weight.kilograms(24.5f),
                ),
                WorkoutSummarySetUi(
                    3,
                    "17.5",
                    11,
                    Weight.kilograms(23.92f),
                    previousOneRm = Weight.kilograms(23.33f),
                ),
            ),
            volumeLoadLabel = "682.5 kg",
            personalRecord = PersonalRecordUi(Weight.kilograms(26.25f), "13 Aug 2026"),
            previous = WorkoutSummaryPreviousUi(
                workoutLabel = "Previous workout: 6 Nov 2026 · 4 days ago",
                topSetOneRm = Weight.kilograms(25.67f),
                averageOneRm = Weight.kilograms(24.5f),
            ),
            oneRmTrend = listOf(
                ExerciseOneRmPointUi(
                    "12 Jul",
                    22.93f,
                    Weight.kilograms(22.93f),
                    "16",
                    12,
                    Weight.kilograms(21.5f),
                ),
                ExerciseOneRmPointUi(
                    "2 Aug",
                    24.83f,
                    Weight.kilograms(24.83f),
                    "18",
                    11,
                    Weight.kilograms(23.4f),
                ),
                ExerciseOneRmPointUi(
                    "13 Aug",
                    28.67f,
                    Weight.kilograms(28.67f),
                    "20",
                    13,
                    Weight.kilograms(26.94f),
                ),
            ),
        ),
    ),
)
