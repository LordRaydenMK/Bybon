package dev.sanastasov.bybon.workout.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.marcellogalhardo.retained.compose.retain
import dev.sanastasov.bybon.ui.components.BybonTopAppBar
import dev.sanastasov.bybon.workout.WorkoutModule
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.catalogExercise

@Composable
fun WorkoutModule.ExerciseDetailsScreen(exerciseId: String, onNavigateBack: () -> Unit) {
    val viewModel = retain {
        ExerciseDetailsViewModel(exerciseId, workoutsRepository, it.coroutineScope)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExerciseDetailsContent(uiState, onNavigateBack)
}

@Composable
private fun ExerciseDetailsContent(
    uiState: ExerciseDetailsUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when (uiState) {
        is ExerciseDetailsUiState.Content -> uiState.name
        ExerciseDetailsUiState.Loading -> "Exercise"
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
                ExerciseDetailsUiState.Loading -> LoadingIndicator()
                is ExerciseDetailsUiState.Content -> DetailsList(uiState)
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
private fun DetailsList(state: ExerciseDetailsUiState.Content) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        detailsImages(state.images)
        detailsChips("facts", state.facts)
        state.personalRecord?.let { detailsPersonalRecord(it) }
        if (state.oneRmTrend.isNotEmpty()) {
            detailsOneRmTrend(state.oneRmTrend, state.personalRecord?.oneRm)
        }
        detailsRecentSessions(state.recentSessions)
        detailsMuscleGroup("Primary muscles", state.primaryMuscles)
        detailsMuscleGroup("Secondary muscles", state.secondaryMuscles)
        state.description?.let { description ->
            item(key = "description") {
                DetailsSection("About") {
                    Text(description, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        detailsList("How to", state.instructions) { index, step -> "${index + 1}. $step" }
        detailsList("Tips", state.tips) { _, tip -> "• $tip" }
        detailsChips("tags", state.tags)
    }
}

@Preview
@Composable
private fun ExerciseDetailsContentPreview() {
    Surface {
        ExerciseDetailsContent(
            catalogExercise("bench-press").toDetailsUi().copy(
                personalRecord = PersonalRecordUi(Weight.kilograms(78f), "20 Sep 2026"),
                oneRmTrend = listOf(
                    ExerciseOneRmPointUi("12 Jul", 70f, Weight.kilograms(70f), "55", 8),
                    ExerciseOneRmPointUi("2 Aug", 74f, Weight.kilograms(74f), "58", 8),
                    ExerciseOneRmPointUi("20 Sep", 78f, Weight.kilograms(78f), "60", 8),
                ),
                recentSessions = listOf(
                    ExerciseDetailsSessionUi(
                        dateLabel = "20 Sep 2026",
                        planName = "Full Body A",
                        sets = listOf(
                            ExerciseDetailsSetUi(1, "60", 8, Weight.kilograms(75f)),
                            ExerciseDetailsSetUi(2, "60", 7, Weight.kilograms(72f)),
                        ),
                    ),
                ),
            ),
            {},
        )
    }
}

@Preview
@Composable
private fun ExerciseDetailsBybonPreview() {
    Surface {
        ExerciseDetailsContent(catalogExercise("iso-lat-row").toDetailsUi(), {})
    }
}
