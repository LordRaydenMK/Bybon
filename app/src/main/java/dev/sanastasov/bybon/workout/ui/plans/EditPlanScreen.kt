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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.marcellogalhardo.retained.compose.retain
import dev.sanastasov.bybon.ui.collectEffectWithLifecycle
import dev.sanastasov.bybon.ui.components.BybonTopAppBar
import dev.sanastasov.bybon.workout.WorkoutModule
import dev.sanastasov.bybon.workout.domain.PlanedExercise
import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.ui.ExerciseOverflow

@Composable
fun WorkoutModule.EditPlanScreen(
    planId: WorkoutPlanId,
    onNavigateBack: () -> Unit,
    onNavigateToExerciseLibrary: () -> Unit,
) {
    val viewModel = retain {
        EditPlanViewModel(planId, workoutsRepository, it.coroutineScope)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    viewModel.effects.collectEffectWithLifecycle { effect ->
        when (effect) {
            EditPlanEffect.NavigateBack -> onNavigateBack()
            EditPlanEffect.OpenExerciseLibrary -> onNavigateToExerciseLibrary()
        }
    }
    EditPlanContent(uiState, viewModel::onAction, onNavigateBack)
}

@Composable
private fun EditPlanContent(
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
                    EditPlanHeader(currentPlan)
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
            onRemoveExercise = {
                onAction(EditPlanAction.OnRemoveExercise(exercise.exercise.id))
            },
            canRemoveExercise = canRemoveExercise,
        )
    }
}

@Composable
private fun EditPlanHeader(plan: WorkoutPlan) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(plan.name, fontWeight = FontWeight.Bold)
        plan.description?.let { Text(it) }
    }
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
