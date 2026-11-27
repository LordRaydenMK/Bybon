package dev.sanastasov.bybon.workout.ui.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
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
    EditPlanContent(uiState, viewModel::onAction, onNavigateBack)
}

@Composable
internal fun EditPlanContent(
    plan: WorkoutPlan?,
    onAction: (EditPlanAction) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        Modifier.fillMaxSize(),
        topBar = { BybonTopAppBar("Edit plan", onNavigateBack) },
    ) { contentPadding ->
        plan?.let { currentPlan ->
            LazyColumn(
                Modifier
                    .padding(contentPadding)
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "header") {
                    EditPlanHeader(currentPlan, onAction)
                }
                itemsIndexed(
                    currentPlan.sets,
                    key = { _, exercise -> exercise.exercise.id },
                ) { index, exercise ->
                    EditPlanExerciseCard(
                        exercise,
                        currentPlan.exerciseOverflow(index, onAction),
                        onAction,
                        canRemoveExercise = currentPlan.sets.size > 1,
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
    }
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

@Composable
private fun EditPlanHeader(plan: WorkoutPlan, onAction: (EditPlanAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EditPlanTextField(
            value = plan.name,
            label = "Name",
            allowBlank = false,
            imeAction = ImeAction.Next,
            onCommit = { onAction(EditPlanAction.OnRenamePlan(it)) },
        )
        EditPlanTextField(
            value = plan.description.orEmpty(),
            label = "Note",
            allowBlank = true,
            imeAction = ImeAction.Done,
            onCommit = { onAction(EditPlanAction.OnUpdateNote(it)) },
            placeholder = "Add a note",
        )
    }
}

@Composable
private fun EditPlanTextField(
    value: String,
    label: String,
    allowBlank: Boolean,
    imeAction: ImeAction,
    onCommit: (String) -> Unit,
    placeholder: String? = null,
) {
    var text by remember(value) { mutableStateOf(value) }
    var focused by remember(value) { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    fun commit() {
        val trimmed = text.trim()
        when {
            trimmed.isEmpty() && !allowBlank -> text = value
            trimmed == value -> if (text != value) text = value
            else -> onCommit(trimmed)
        }
    }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                val wasFocused = focused
                focused = focusState.isFocused
                if (wasFocused && !focusState.isFocused) commit()
            },
        textStyle = MaterialTheme.typography.bodyLarge,
        label = { Text(label) },
        placeholder = placeholder?.let { hint -> { Text(hint) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) },
            onDone = {
                focusManager.clearFocus()
                keyboardController?.hide()
            },
        ),
    )
}

@Preview
@Composable
private fun EditPlanContentPreview() {
    Surface {
        EditPlanContent(fullBodyA, {}, {})
    }
}

@Preview
@Composable
private fun EditPlanContentSingleExercisePreview() {
    Surface {
        EditPlanContent(fullBodyA.copy(sets = listOf(fullBodyA.sets.first())), {}, {})
    }
}
