package dev.sanastasov.bybon.workout.ui.plans

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.marcellogalhardo.retained.compose.retain
import dev.sanastasov.bybon.ui.components.BybonTopAppBar
import dev.sanastasov.bybon.workout.WorkoutModule
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB

@Composable
fun WorkoutModule.EditPlanScreen(planId: WorkoutPlanId, onNavigateBack: () -> Unit) {
    val viewModel = retain {
        EditPlanViewModel(planId, workoutsRepository, it.coroutineScope)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    EditPlanContent(uiState, onNavigateBack)
}

@Composable
private fun EditPlanContent(planUi: WorkoutPlanUi?, onNavigateBack: () -> Unit) {
    Scaffold(
        Modifier.fillMaxSize(),
        topBar = { BybonTopAppBar("Edit plan", onNavigateBack) },
    ) { contentPadding ->
        planUi?.let {
            Column(
                Modifier
                    .padding(contentPadding)
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .fillMaxSize(),
            ) {
                Card(Modifier.fillMaxWidth()) {
                    WorkoutPlanInfo(
                        it,
                        Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun EditPlanContentActivePreview() {
    Surface {
        EditPlanContent(WorkoutPlanUi(fullBodyA, isActive = true), {})
    }
}

@Preview
@Composable
private fun EditPlanContentInactivePreview() {
    Surface {
        EditPlanContent(WorkoutPlanUi(fullBodyB, isActive = false), {})
    }
}
