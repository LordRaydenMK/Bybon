package dev.sanastasov.bybon.workout.ui.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.fullBodyA

@Composable
fun WorkoutModule.EditPlanScreen(planId: WorkoutPlanId, onNavigateBack: () -> Unit) {
    val viewModel = retain {
        EditPlanViewModel(planId, workoutsRepository, it.coroutineScope)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    viewModel.effects.collectEffectWithLifecycle { effect ->
        when (effect) {
            EditPlanEffect.NavigateBack -> onNavigateBack()
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
        plan?.let {
            Column(
                Modifier
                    .padding(contentPadding)
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .fillMaxSize(),
            ) {
                EditPlanHeader(it)
                Spacer(Modifier.height(16.dp))
                LazyColumn(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    itemsIndexed(
                        it.sets,
                        key = { index, exercise -> "${exercise.exercise.id}-$index" },
                    ) { _, exercise ->
                        Card(Modifier.fillMaxWidth()) {
                            PlannedExerciseCard(exercise)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                TextButton(
                    { onAction(EditPlanAction.OnArchivePlan) },
                    Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Archive Plan")
                }
            }
        }
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
