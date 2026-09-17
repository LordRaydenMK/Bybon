package dev.sanastasov.bybon.workout.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.marcellogalhardo.retained.compose.retain
import dev.sanastasov.bybon.ui.collectEffectWithLifecycle
import dev.sanastasov.bybon.ui.components.BybonTopAppBar
import dev.sanastasov.bybon.workout.WorkoutModule
import dev.sanastasov.bybon.workout.domain.MuscleGroup
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.catalogExercises

@Composable
fun WorkoutModule.ExerciseLibraryScreen(planId: WorkoutPlanId, onNavigateBack: () -> Unit) {
    val viewModel = retain {
        ExerciseLibraryViewModel(planId, workoutsRepository, it.coroutineScope)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    viewModel.effects.collectEffectWithLifecycle { effect ->
        when (effect) {
            ExerciseLibraryEffect.NavigateBack -> onNavigateBack()
        }
    }
    ExerciseLibraryContent(uiState, viewModel::onAction, onNavigateBack)
}

@Composable
private fun ExerciseLibraryContent(
    state: ExerciseLibraryUiState,
    onAction: (ExerciseLibraryAction) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        Modifier.fillMaxSize(),
        topBar = { BybonTopAppBar("Exercise Library", onNavigateBack) },
        bottomBar = {
            AddExerciseBar(state.addEnabled) {
                state.selectedExerciseId?.let { onAction(ExerciseLibraryAction.OnAddExercise(it)) }
            }
        },
    ) { contentPadding ->
        LazyColumn(
            Modifier
                .padding(contentPadding)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.groups.forEach { group ->
                item(key = "header-${group.bodyPart}") {
                    BodyPartHeader(group.bodyPart)
                }
                items(group.exercises, key = { it.id }) { exercise ->
                    Card(Modifier.fillMaxWidth()) {
                        ExerciseLibraryCard(exercise, onAction)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddExerciseBar(enabled: Boolean, onClick: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Button(
            onClick,
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            enabled = enabled,
        ) {
            Text("Add exercise")
        }
    }
}

@Composable
private fun BodyPartHeader(bodyPart: MuscleGroup) {
    Text(
        bodyPart.name,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun ExerciseLibraryCard(
    exercise: ExerciseLibraryItemUi,
    onAction: (ExerciseLibraryAction) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                exercise.name,
                fontWeight = FontWeight.Bold,
            )
            Text(
                exercise.equipmentLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Checkbox(
            exercise.selected,
            { onAction(ExerciseLibraryAction.OnToggleExercise(exercise.id)) },
            Modifier.clearAndSetSemantics {
                contentDescription = if (exercise.selected) {
                    "Deselect ${exercise.name}"
                } else {
                    "Select ${exercise.name}"
                }
            },
        )
    }
}

@Preview
@Composable
private fun ExerciseLibraryContentPreview() {
    Surface {
        ExerciseLibraryContent(
            ExerciseLibraryUiState(
                catalogExercises.groupedByBodyPart(catalogExercises.first().id),
            ),
            {},
            {},
        )
    }
}
