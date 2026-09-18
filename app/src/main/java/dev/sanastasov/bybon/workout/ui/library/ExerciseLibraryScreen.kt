package dev.sanastasov.bybon.workout.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.marcellogalhardo.retained.compose.retain
import dev.sanastasov.bybon.ui.collectEffectWithLifecycle
import dev.sanastasov.bybon.ui.components.BybonTopAppBar
import dev.sanastasov.bybon.ui.icons.icon
import dev.sanastasov.bybon.workout.WorkoutModule
import dev.sanastasov.bybon.workout.domain.Equipment
import dev.sanastasov.bybon.workout.domain.MuscleGroup
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.catalogExercises
import dev.sanastasov.bybon.workout.domain.fullBodyA

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
        Column(
            Modifier
                .padding(contentPadding)
                .fillMaxSize(),
        ) {
            FilterChipBar(
                chips = state.filterChips,
                onAction = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                exerciseLibraryItems(state, onAction)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChipBar(
    chips: List<ExerciseLibraryFilterChipUi>,
    onAction: (ExerciseLibraryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { chip ->
            key(chip.id) {
                FilterChipItem(chip, onAction)
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    chip: ExerciseLibraryFilterChipUi,
    onAction: (ExerciseLibraryAction) -> Unit,
) {
    val onClick = { onAction(ExerciseLibraryAction.OnToggleFilter(chip.id)) }
    if (chip.id is ExerciseLibraryFilterId.ClearAll) {
        AssistChip(
            onClick = onClick,
            label = { Text(chip.label) },
        )
    } else {
        FilterChip(
            selected = chip.selected,
            onClick = onClick,
            label = { Text(chip.label) },
        )
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

private fun LazyListScope.exerciseLibraryItems(
    state: ExerciseLibraryUiState,
    onAction: (ExerciseLibraryAction) -> Unit,
) {
    state.inPlanHeader?.let { header ->
        item(key = "header-in-plan") {
            Text(
                header,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        items(state.inPlanExercises, key = { "in-plan-${it.id}" }) { exercise ->
            val colors = if (exercise.selectable) {
                CardDefaults.cardColors()
            } else {
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                )
            }
            Card(
                Modifier.fillMaxWidth(),
                colors = colors,
            ) {
                ExerciseLibraryCard(exercise, onAction)
            }
        }
    }
    if (state.showEmptyState) {
        item(key = "empty-filter") {
            Text(
                "No matching exercises",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    } else {
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
        EquipmentIcon(exercise.equipment)
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
        if (exercise.selectable) {
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
}

private val EquipmentIconSize = 40.dp

@Composable
private fun EquipmentIcon(equipment: Equipment) {
    Icon(
        equipment.icon,
        contentDescription = null,
        Modifier
            .size(EquipmentIconSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview
@Composable
private fun ExerciseLibraryContentPreview() {
    Surface {
        ExerciseLibraryContent(
            catalogExercises.toLibraryUiState(
                selectedExerciseId = "incline-curl-db",
                planName = fullBodyA.name,
                planExercises = fullBodyA.sets.map { it.exercise },
            ),
            {},
            {},
        )
    }
}

@Preview
@Composable
private fun ExerciseLibraryEmptyFilterPreview() {
    Surface {
        ExerciseLibraryContent(
            catalogExercises.toLibraryUiState(
                filters = ExerciseLibraryFilters(muscleGroups = setOf(MuscleGroup.Core)),
            ),
            {},
            {},
        )
    }
}
