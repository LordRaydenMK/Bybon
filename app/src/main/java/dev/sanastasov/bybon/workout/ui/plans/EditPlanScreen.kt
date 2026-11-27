package dev.sanastasov.bybon.workout.ui.plans

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.ResultEffect
import dev.marcellogalhardo.retained.compose.retain
import dev.sanastasov.bybon.ui.collectEffectWithLifecycle
import dev.sanastasov.bybon.ui.components.BybonTopAppBar
import dev.sanastasov.bybon.ui.defaultScope
import dev.sanastasov.bybon.workout.WorkoutModule
import dev.sanastasov.bybon.workout.domain.PlanedExercise
import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.ui.ExerciseOverflow
import dev.sanastasov.bybon.workout.ui.library.EXERCISE_LIBRARY_RESULT_KEY

@Composable
fun WorkoutModule.EditPlanScreen(
    planId: WorkoutPlanId,
    onNavigateBack: () -> Unit,
    onNavigateToExerciseLibrary: (List<String>) -> Unit,
) {
    val viewModel = retain {
        EditPlanViewModel(planId, workoutsRepository, it.defaultScope())
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val resultBus = LocalResultEventBus.current
    ResultEffect<String>(resultKey = EXERCISE_LIBRARY_RESULT_KEY) { exerciseId ->
        resultBus.removeResult(resultKey = EXERCISE_LIBRARY_RESULT_KEY)
        viewModel.onAction(EditPlanAction.OnExercisePicked(exerciseId))
    }
    viewModel.effects.collectEffectWithLifecycle { effect ->
        when (effect) {
            EditPlanEffect.NavigateBack -> onNavigateBack()

            is EditPlanEffect.OpenExerciseLibrary ->
                onNavigateToExerciseLibrary(effect.existingExerciseIds)
        }
    }
    BackHandler(enabled = uiState?.canSave == true && uiState?.confirmDiscard == false) {
        viewModel.onAction(EditPlanAction.OnBack)
    }
    EditPlanContent(uiState, viewModel::onAction)
}

@Composable
internal fun EditPlanContent(state: EditPlanUiState?, onAction: (EditPlanAction) -> Unit) {
    Scaffold(
        Modifier.fillMaxSize(),
        topBar = { EditPlanTopBar(state?.canSave == true, onAction) },
    ) { contentPadding ->
        state?.let { current ->
            EditPlanList(current, onAction, Modifier.padding(contentPadding))
        }
    }
    if (state?.confirmDiscard == true) {
        DiscardPlanEditsDialog(onAction)
    }
}

@Composable
private fun EditPlanTopBar(canSave: Boolean, onAction: (EditPlanAction) -> Unit) {
    BybonTopAppBar(
        "Edit plan",
        { onAction(EditPlanAction.OnBack) },
        actions = {
            TextButton(
                { onAction(EditPlanAction.OnSave) },
                enabled = canSave,
            ) {
                Text("Save")
            }
        },
    )
}

@Composable
private fun EditPlanList(
    state: EditPlanUiState,
    onAction: (EditPlanAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val plan = state.plan
    LazyColumn(
        modifier
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header") {
            EditPlanHeader(plan, onAction)
        }
        itemsIndexed(
            plan.sets,
            key = { _, exercise -> exercise.exercise.id },
        ) { index, exercise ->
            EditPlanExerciseCard(
                exercise,
                plan.exerciseOverflow(index, onAction),
                onAction,
                canRemoveExercise = plan.sets.size > 1,
            )
        }
        item(key = "add-exercise") {
            Box(
                Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Button({ onAction(EditPlanAction.OnAddExercise) }) {
                    Text("Add Exercise")
                }
            }
        }
        item(key = "archive") {
            Box(
                Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                TextButton({ onAction(EditPlanAction.OnArchivePlan) }) {
                    Text("Archive Plan")
                }
            }
        }
    }
}

@Composable
private fun EditPlanHeader(plan: WorkoutPlan, onAction: (EditPlanAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            plan.name,
            { onAction(EditPlanAction.OnNameChanged(it)) },
            Modifier.fillMaxWidth(),
            label = { Text("Name") },
            singleLine = true,
        )
        OutlinedTextField(
            plan.description.orEmpty(),
            { onAction(EditPlanAction.OnNoteChanged(it)) },
            Modifier.fillMaxWidth(),
            label = { Text("Note") },
            singleLine = true,
        )
    }
}

@Composable
private fun DiscardPlanEditsDialog(onAction: (EditPlanAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(EditPlanAction.OnDiscardDismissed) },
        title = { Text("Discard changes?") },
        text = { Text("Unsaved edits to this plan will be lost.") },
        confirmButton = {
            TextButton({ onAction(EditPlanAction.OnDiscardConfirmed) }) {
                Text("Discard")
            }
        },
        dismissButton = {
            TextButton({ onAction(EditPlanAction.OnDiscardDismissed) }) {
                Text("Keep editing")
            }
        },
    )
}

@Composable
private fun LazyItemScope.EditPlanExerciseCard(
    exercise: PlanedExercise,
    overflow: ExerciseOverflow?,
    onAction: (EditPlanAction) -> Unit,
    canRemoveExercise: Boolean,
) {
    Card(
        Modifier
            .fillMaxWidth()
            .animateItem(),
    ) {
        PlannedExerciseCard(
            exercise,
            overflow = overflow,
            onAddSet = { onAction(EditPlanAction.OnAddSet(exercise.exercise.id)) },
            onRemoveLastSet = {
                onAction(EditPlanAction.OnRemoveLastSet(exercise.exercise.id))
            },
            onAddWarmupSet = { onAction(EditPlanAction.OnAddWarmupSet(exercise.exercise.id)) },
            onRemoveLastWarmupSet = {
                onAction(EditPlanAction.OnRemoveLastWarmupSet(exercise.exercise.id))
            },
            onRemoveExercise = {
                onAction(EditPlanAction.OnRemoveExercise(exercise.exercise.id))
            },
            canRemoveExercise = canRemoveExercise,
        )
    }
}

@Preview
@Composable
private fun EditPlanContentPreview() {
    Surface {
        EditPlanContent(EditPlanUiState(fullBodyA, canSave = false), {})
    }
}

@Preview
@Composable
private fun EditPlanContentSingleExercisePreview() {
    Surface {
        EditPlanContent(
            EditPlanUiState(fullBodyA.copy(sets = listOf(fullBodyA.sets.first())), canSave = false),
            {},
        )
    }
}
