package dev.sanastasov.bybon.workout.ui.summary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.marcellogalhardo.retained.compose.retain
import dev.sanastasov.bybon.ui.collectEffectWithLifecycle
import dev.sanastasov.bybon.ui.components.BybonTopAppBar
import dev.sanastasov.bybon.workout.WorkoutModule
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutSessionId
import dev.sanastasov.bybon.workout.ui.MISSING_LOAD_LABEL
import dev.sanastasov.bybon.workout.ui.SessionMeta
import dev.sanastasov.bybon.workout.ui.WarmupLoadRow
import dev.sanastasov.bybon.workout.ui.details.ExerciseOneRmPointUi
import dev.sanastasov.bybon.workout.ui.details.ExerciseOneRmTrendCard
import dev.sanastasov.bybon.workout.ui.details.PersonalRecordUi
import dev.sanastasov.bybon.workout.ui.formatLoadedSet
import dev.sanastasov.bybon.workout.ui.formatOneRmDelta

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
        item(key = "session-meta") {
            SessionMeta(state.date, state.startTime, state.duration)
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
                    ExerciseSummaryCard(exercise, onAction)
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

@Composable
private fun ExerciseSummaryCard(
    exercise: WorkoutSummaryExerciseUi,
    onAction: (WorkoutSummaryAction) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            exercise.name,
            Modifier
                .fillMaxWidth()
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Open exercise details",
                    onClick = {
                        onAction(WorkoutSummaryAction.OnOpenExerciseDetails(exercise.id))
                    },
                ),
            fontWeight = FontWeight.Bold,
        )
        exercise.notes.forEach { note ->
            Text(
                note,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(4.dp))
        val workSets = exercise.sets.filterNot { it.isWarmup }
        exercise.sets.filter { it.isWarmup }.forEach { set ->
            WarmupLoadRow(
                formatLoadedSet(set.weightKg, set.reps),
                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        if (workSets.isNotEmpty()) {
            SetLoadOneRmHeader()
            workSets.forEach { set ->
                WorkSetRow(set)
            }
        }
        exercise.personalRecord?.let { personalRecord ->
            if (workSets.isEmpty()) SetLoadOneRmHeader()
            AllTimeOneRmRow(personalRecord)
        }
    }
}

@Composable
private fun WorkSetRow(set: WorkoutSummarySetUi) {
    val number = checkNotNull(set.number)
    SetLoadOneRmRow(
        leading = "$number.",
        weightKg = set.weightKg,
        reps = set.reps,
        oneRm = set.oneRm,
        rowDescription = set.contentDescription(),
        previousOneRm = set.previousOneRm,
    )
}

internal fun WorkoutSummarySetUi.contentDescription(): String {
    val number = checkNotNull(number)
    val load = formatLoadedSet(weightKg, reps)
    val rm = oneRm?.let { "${it.kilograms} kg 1RM" } ?: MISSING_LOAD_LABEL
    return "$number. $load, $rm, ${formatOneRmDelta(oneRm, previousOneRm)}"
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
    duration = "00:47",
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
            personalRecord = PersonalRecordUi(Weight.kilograms(63f), "13 Aug 2026"),
            oneRmTrend = listOf(
                ExerciseOneRmPointUi("12 Jul", 55f, Weight.kilograms(55f), "40", 10),
                ExerciseOneRmPointUi("2 Aug", 58f, Weight.kilograms(58f), "42", 10),
                ExerciseOneRmPointUi("13 Aug", 63f, Weight.kilograms(63f), "45", 12),
            ),
        ),
        WorkoutSummaryExerciseUi(
            id = "incline-db-press",
            name = "Incline Dumbbell Press",
            notes = listOf("Rep range 11-15"),
            sets = listOf(
                WorkoutSummarySetUi(
                    1,
                    "20",
                    13,
                    Weight.kilograms(28.67f),
                    previousOneRm = Weight.kilograms(22.93f),
                ),
                WorkoutSummarySetUi(
                    2,
                    "20",
                    11,
                    Weight.kilograms(27.33f),
                    previousOneRm = Weight.kilograms(21.33f),
                ),
                WorkoutSummarySetUi(
                    3,
                    "20",
                    8,
                    Weight.kilograms(24.83f),
                    previousOneRm = Weight.kilograms(19.86f),
                ),
            ),
            personalRecord = PersonalRecordUi(Weight.kilograms(28.67f), "13 Aug 2026"),
            oneRmTrend = listOf(
                ExerciseOneRmPointUi("12 Jul", 22.93f, Weight.kilograms(22.93f), "16", 12),
                ExerciseOneRmPointUi("2 Aug", 24.83f, Weight.kilograms(24.83f), "18", 11),
                ExerciseOneRmPointUi("13 Aug", 28.67f, Weight.kilograms(28.67f), "20", 13),
            ),
        ),
    ),
)
