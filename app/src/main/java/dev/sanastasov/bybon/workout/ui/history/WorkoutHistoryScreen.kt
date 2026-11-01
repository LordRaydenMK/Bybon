@file:Suppress("TooManyFunctions")

package dev.sanastasov.bybon.workout.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.sanastasov.bybon.domain.toDisplayDate
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSessionId
import dev.sanastasov.bybon.workout.domain.catalogExercise
import dev.sanastasov.bybon.workout.domain.estimateOneRmKg
import dev.sanastasov.bybon.workout.ui.MISSING_LOAD_LABEL
import dev.sanastasov.bybon.workout.ui.SessionMeta
import dev.sanastasov.bybon.workout.ui.formatElapsedClock
import dev.sanastasov.bybon.workout.ui.formatLoadedSet
import dev.sanastasov.bybon.workout.ui.toContentLoadLabel
import dev.sanastasov.bybon.workout.ui.toLoadLabel
import dev.sanastasov.bybon.workout.ui.toStartTimeLabel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Duration.Companion.seconds

@Composable
fun WorkoutHistoryTab(
    uiState: WorkoutHistoryUiState,
    onSessionClick: (WorkoutSessionId) -> Unit,
    onImportHistoryClick: () -> Unit,
    onImportDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        when (uiState) {
            WorkoutHistoryUiState.Loading -> LoadingIndicator()
            WorkoutHistoryUiState.Empty -> EmptyHistory(onImportHistoryClick)
            WorkoutHistoryUiState.Importing -> ImportingIndicator()
            is WorkoutHistoryUiState.Summary -> ImportSummary(uiState.summary, onImportDone)
            is WorkoutHistoryUiState.History -> HistoryList(uiState.sessions, onSessionClick)
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
private fun ImportSummary(summary: ImportSummaryUi, onImportDone: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Import summary",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "${summary.sessionCount} ${sessionsLabel(summary.sessionCount)} imported",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        if (summary.sessionsSkipped > 0) {
            Text(
                "${summary.sessionsSkipped} ${sessionsLabel(
                    summary.sessionsSkipped,
                )} already in history",
            )
        }
        Text(
            "Sessions by plan",
            fontWeight = FontWeight.Bold,
        )
        summary.sessionsByPlan.forEach { plan ->
            Text("${plan.planName}: ${plan.sessionCount}")
        }
        Text("${summary.plansCreatedCount} ${plansLabel(summary.plansCreatedCount)} created")
        Text("${summary.plansArchivedCount} ${plansLabel(summary.plansArchivedCount)} archived")
        Text(
            "${summary.exercisesImportedCount} ${exercisesLabel(
                summary.exercisesImportedCount,
            )} imported",
        )
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
private fun HistoryList(
    sessions: List<WorkoutSessionHistoryUi>,
    onSessionClick: (WorkoutSessionId) -> Unit,
) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(sessions, key = { it.id.toString() }) { session ->
            WorkoutSessionHistoryCard(session, onSessionClick)
        }
    }
}

@Composable
private fun WorkoutSessionHistoryCard(
    session: WorkoutSessionHistoryUi,
    onSessionClick: (WorkoutSessionId) -> Unit,
) {
    Card(
        onClick = { onSessionClick(session.id) },
        modifier = Modifier.fillMaxWidth(),
    ) {
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
            SessionMeta(
                date = session.date.toDisplayDate(),
                startTime = session.startTime,
                duration = session.duration,
            )
            Spacer(Modifier.height(4.dp))
            SetLoadOneRmHeader()
            session.exercises.forEach { exercise ->
                TopSetRow(exercise)
            }
        }
    }
}

private val DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

private fun sessionsLabel(count: Int): String = if (count == 1) "session" else "sessions"

private fun plansLabel(count: Int): String = if (count == 1) "plan" else "plans"

private fun exercisesLabel(count: Int): String = if (count == 1) "exercise" else "exercises"

@Composable
private fun TopSetRow(exercise: ExerciseTopSetUi) {
    SetLoadOneRmRow(
        leading = exercise.name,
        weightKg = exercise.weightKg,
        reps = exercise.reps,
        oneRm = exercise.oneRm,
        rowDescription = exercise.contentDescription(),
    )
}

internal fun ExerciseTopSetUi.contentDescription(): String {
    val load = formatLoadedSet(weightKg, reps)
    val rm = oneRm?.let { "${it.kilograms} kg 1RM" } ?: MISSING_LOAD_LABEL
    return "$name, $load, $rm"
}

private val LoadColumnMinWidth = 96.dp
private val OneRmColumnMinWidth = 72.dp

@Composable
private fun SetLoadOneRmHeader(modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HeaderCell("Exercise", Modifier.weight(1f), TextAlign.Start)
        HeaderCell("kg x reps", Modifier.widthIn(min = LoadColumnMinWidth), TextAlign.End)
        HeaderCell("1RM", Modifier.widthIn(min = OneRmColumnMinWidth), TextAlign.End)
    }
}

@Composable
private fun SetLoadOneRmRow(
    leading: String,
    weightKg: String,
    reps: Int,
    oneRm: Weight?,
    rowDescription: String,
    modifier: Modifier = Modifier,
) {
    val load = formatLoadedSet(weightKg, reps)
    val oneRmLabel = oneRm.toContentLoadLabel()
    Row(
        modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = rowDescription },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            leading,
            Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            load,
            Modifier.widthIn(min = LoadColumnMinWidth),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
        )
        Text(
            oneRmLabel,
            Modifier.widthIn(min = OneRmColumnMinWidth),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
        )
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier, textAlign: TextAlign) {
    Text(
        text,
        modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium,
        textAlign = textAlign,
    )
}

@Preview
@Composable
private fun WorkoutHistoryContentPreview() {
    Surface {
        WorkoutHistoryTab(
            uiState = WorkoutHistoryUiState.History(
                listOf(
                    previewHistorySession(
                        planId = "upper-body-b",
                        planName = "Upper body B",
                        startedAt = LocalDateTime.of(2026, 8, 20, 17, 59),
                        durationSeconds = 3367,
                        previewTopSet("incline-db-press", 20f, 14),
                        previewTopSet("chest-supported-db-row", 22f, 15),
                        previewTopSet("incline-db-curl", 9f, 15),
                        previewTopSet("lateral-raise-machine", 32.5f, 12),
                        previewTopSet("machine-seated-crunch", 35f, 15),
                    ),
                    previewHistorySession(
                        planId = "upper-body-a",
                        planName = "Upper body A",
                        startedAt = LocalDateTime.of(2026, 8, 18, 18, 12),
                        durationSeconds = 3313,
                        previewTopSet("bench-press", 60f, 8),
                        previewTopSet("assisted-pull-ups", 12.5f, 8),
                        previewTopSet("dumbbell-upright-row", 16f, 11),
                        previewTopSet("db-skull-crusher", 9f, 16),
                        previewTopSet("machine-seated-crunch", 35f, 15),
                    ),
                    previewHistorySession(
                        planId = "full-body-b",
                        planName = "Full Body B",
                        startedAt = LocalDateTime.of(2026, 8, 13, 18, 25),
                        durationSeconds = 2864,
                        previewTopSet("romanian-deadlift", 45f, 12),
                        previewTopSet("incline-db-press", 20f, 13),
                        previewTopSet("bulgarian-split-squat", 14f, 10),
                        previewTopSet("chest-supported-db-row", 22f, 14),
                    ),
                    previewHistorySession(
                        planId = "full-body-a",
                        planName = "Full Body A",
                        startedAt = LocalDateTime.of(2026, 8, 10, 11, 54),
                        durationSeconds = 3877,
                        previewTopSet("bench-press", 57.5f, 10),
                        previewTopSet("squat", 45f, 12),
                        previewTopSet("assisted-pull-ups", 12.5f, 10),
                        previewTopSet("seated-leg-curl", 32.5f, 16),
                        previewTopSet("dumbbell-upright-row", 16f, 10),
                        previewTopSet("triceps-press-machine", 57.5f, 15),
                    ),
                ),
            ),
            onSessionClick = {},
            onImportHistoryClick = {},
            onImportDone = {},
        )
    }
}

private fun previewHistorySession(
    planId: String,
    planName: String,
    startedAt: LocalDateTime,
    durationSeconds: Int,
    vararg exercises: ExerciseTopSetUi,
) = WorkoutSessionHistoryUi(
    id = WorkoutSessionId(WorkoutPlanId(planId), startedAt),
    planName = planName,
    date = startedAt.toLocalDate(),
    startTime = startedAt.toStartTimeLabel(),
    duration = durationSeconds.seconds.formatElapsedClock(),
    exercises = exercises.toList(),
)

private fun previewTopSet(exerciseId: String, kg: Float, reps: Int): ExerciseTopSetUi {
    val weight = Weight.kilograms(kg)
    return ExerciseTopSetUi(
        name = catalogExercise(exerciseId).name,
        weightKg = weight.toLoadLabel(),
        reps = reps,
        oneRm = Weight.kilograms(estimateOneRmKg(kg, reps)),
    )
}

@Preview
@Composable
private fun WorkoutHistoryEmptyPreview() {
    Surface {
        WorkoutHistoryTab(
            uiState = WorkoutHistoryUiState.Empty,
            onSessionClick = {},
            onImportHistoryClick = {},
            onImportDone = {},
        )
    }
}

@Preview
@Composable
private fun WorkoutHistoryImportSummaryPreview() {
    Surface {
        WorkoutHistoryTab(
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
                    plansArchivedCount = 2,
                    exercisesImportedCount = 1,
                    firstSessionDate = LocalDate.of(2026, 2, 17),
                    lastSessionDate = LocalDate.of(2026, 8, 20),
                    workingSetCount = 825,
                    sessionsSkipped = 0,
                ),
            ),
            onSessionClick = {},
            onImportHistoryClick = {},
            onImportDone = {},
        )
    }
}
